/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation.codegen;

/**
 * Pure-shape source generation over the shared packed/borrowed f-tree column contract.
 * No storage owner, native address, query constant, or engine slot enters the compiled shape.
 * Parent ordinals precede their children. This is also usable by non-IR topology consumers.
 */
public final class PackedFtreeKernelSource {
	private PackedFtreeKernelSource() { }

	private static void validate(int[][] children, int root, String name) {
		if (children == null || children.length == 0 || root != 0)
			throw new IllegalArgumentException("a root-first non-empty tree is required");
		if (name == null || name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0)))
			throw new IllegalArgumentException("invalid generated class name");
		for (int i = 1; i < name.length(); i++) {
			if (!Character.isJavaIdentifierPart(name.charAt(i))) throw new IllegalArgumentException("invalid generated class name");
		}
		boolean[] seen = new boolean[children.length]; seen[root] = true;
		for (int parent = 0; parent < children.length; parent++) {
			if (!seen[parent] || children[parent] == null) throw new IllegalArgumentException("disconnected tree");
			for (int child : children[parent]) {
				if (child <= parent || child >= children.length || seen[child])
					throw new IllegalArgumentException("non-tree or non-topological edge");
				seen[child] = true;
			}
		}
	}

	public static String source(int[][] children, int root, String simpleName) {
		validate(children, root, simpleName);
		StringBuilder s = new StringBuilder(16_384);
		s.append("package org.eclipse.rdf4j.sail.lmdb.evaluation.codegen;\n")
				.append("public final class ")
				.append(simpleName)
				.append(" implements PackedFtreeKernel {\n")
				.append("  private PackedFtreeContext p; private boolean done;\n")
				.append("  public void bind(KernelContext c){ p=c.packedFtree; done=false; }\n")
				.append("  private static int next(long[] b,int from,int end){ if(from>=end)return -1; int w=from>>>6; long x=b[w]&(-1L<<(from&63)); for(;;){ if(x!=0){int v=(w<<6)+Long.numberOfTrailingZeros(x); return v<end?v:-1;} if(((++w)<<6)>=end)return -1; x=b[w]; }}\n")
				.append("  private static boolean set(long[] b,int i){ return (b[i>>>6]&(1L<<(i&63)))!=0; }\n")
				.append("  private static long w(long[] a,int i){ return a==null?1L:(a[i]==0L?1L:a[i]); }\n")
				.append("  public int fill(long[] ignored,int maxRows){ if(done)return 0; done=true;\n");

		for (int ordinal = children.length - 1; ordinal >= 0; ordinal--) {
			emitSubtree(s, ordinal, children);
		}
		s.append("    long total=0L; { long[] sel=p.selectors[")
				.append(root)
				.append("], sub=p.subtreeCounts[")
				.append(root)
				.append("]; int end=p.ends[")
				.append(root)
				.append("]; for(int i=next(sel,p.starts[")
				.append(root)
				.append("],end);i>=0;i=next(sel,i+1,end)) total=PackedFtreeMath.add(total,sub[i]); } p.totalRows=total; if(!p.needOutsideCounts)return 0;\n");
		for (int i = 0; i < children.length; i++) {
			s.append("    java.util.Arrays.fill(p.outsideCounts[")
					.append(i)
					.append("],0,p.sizes[")
					.append(i)
					.append("],0L);\n");
		}
		s.append("    { long[] sel=p.selectors[")
				.append(root)
				.append("], out=p.outsideCounts[")
				.append(root)
				.append("]; int end=p.ends[")
				.append(root)
				.append("]; for(int i=next(sel,p.starts[")
				.append(root)
				.append("],end);i>=0;i=next(sel,i+1,end)) out[i]=1L; }\n");
		for (int node = 0; node < children.length; node++) {
			if (children[node].length > 0) {
				emitOutside(s, node, children);
			}
		}
		s.append("    return 0; }\n}");
		return s.toString();
	}

	private static void emitSubtree(StringBuilder s, int node, int[][] children) {
		int n = node;
		s.append("    { long[] sel=p.selectors[")
				.append(n)
				.append("], wt=p.weights[")
				.append(n)
				.append("], sub=p.subtreeCounts[")
				.append(n)
				.append("]; int end=p.ends[")
				.append(n)
				.append("]; java.util.Arrays.fill(sub,0,p.sizes[")
				.append(n)
				.append("],0L); for(int i=next(sel,p.starts[")
				.append(n)
				.append("],end);i>=0;i=next(sel,i+1,end)){ long c=w(wt,i);\n");
		for (int c : children[node]) {
			s.append("      long s")
					.append(c)
					.append("=0L; if(p.borrowedCounts[")
					.append(c)
					.append("]!=null) s")
					.append(c)
					.append("=p.borrowedCounts[")
					.append(c)
					.append("][i]; else if(p.sharedWithParent[")
					.append(c)
					.append("]){ if(set(p.selectors[")
					.append(c)
					.append("],i)) s")
					.append(c)
					.append("=p.subtreeCounts[")
					.append(c)
					.append("][i]; } else { int f=p.offsets[")
					.append(c)
					.append("][i], t=p.offsets[")
					.append(c)
					.append("][i+1]; long[] cs=p.selectors[")
					.append(c)
					.append("], cv=p.subtreeCounts[")
					.append(c)
					.append("]; for(int j=next(cs,f,t);j>=0&&j<t;j=next(cs,j+1,t)) s")
					.append(c)
					.append("=PackedFtreeMath.add(s")
					.append(c)
					.append(",cv[j]); } if(s")
					.append(c)
					.append("==0L){c=0L;} else if(c!=0L)c=PackedFtreeMath.multiply(c,s")
					.append(c)
					.append(");\n");
		}
		s.append("      sub[i]=c; } }\n");
	}

	private static void emitOutside(StringBuilder s, int node, int[][] children) {
		int n = node;
		int childCount = children[node].length;
		s.append("    { long[] psel=p.selectors[")
				.append(n)
				.append("], pout=p.outsideCounts[")
				.append(n)
				.append("], pwt=p.weights[")
				.append(n)
				.append("]; int pend=p.ends[")
				.append(n)
				.append("], k=")
				.append(childCount)
				.append("; long[] sums=new long[k], pre=new long[k], suf=new long[k];")
				.append(" for(int i=next(psel,p.starts[")
				.append(n)
				.append("],pend);i>=0;i=next(psel,i+1,pend)){ long base=pout[i]; if(base==0L)continue; base=PackedFtreeMath.multiply(base,w(pwt,i));\n");
		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			int c = children[node][childIndex];
			s.append("      long s")
					.append(c)
					.append("=0L; if(p.borrowedCounts[")
					.append(c)
					.append("]!=null) s")
					.append(c)
					.append("=p.borrowedCounts[")
					.append(c)
					.append("][i]; else if(p.sharedWithParent[")
					.append(c)
					.append("]){ if(set(p.selectors[")
					.append(c)
					.append("],i)) s")
					.append(c)
					.append("=p.subtreeCounts[")
					.append(c)
					.append("][i]; } else { int f=p.offsets[")
					.append(c)
					.append("][i], t=p.offsets[")
					.append(c)
					.append("][i+1]; long[] cs=p.selectors[")
					.append(c)
					.append("], cv=p.subtreeCounts[")
					.append(c)
					.append("]; for(int j=next(cs,f,t);j>=0&&j<t;j=next(cs,j+1,t)) s")
					.append(c)
					.append("=PackedFtreeMath.add(s")
					.append(c)
					.append(",cv[j]); } sums[")
					.append(childIndex)
					.append("]=s")
					.append(c)
					.append(";\n");
		}
		s.append("      pre[0]=1L; for(int q=1;q<k;q++)pre[q]=PackedFtreeMath.multiply(pre[q-1],sums[q-1]);")
				.append(" suf[k-1]=1L; for(int q=k-2;q>=0;q--)suf[q]=PackedFtreeMath.multiply(sums[q+1],suf[q+1]);\n");
		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			int c = children[node][childIndex];
			s.append("      { long o=PackedFtreeMath.multiply(base,PackedFtreeMath.multiply(pre[")
					.append(childIndex)
					.append("],suf[")
					.append(childIndex)
					.append("]));")
					.append(" if(p.borrowedCounts[")
					.append(c)
					.append("]!=null){} else if(p.sharedWithParent[")
					.append(c)
					.append("]){ if(set(p.selectors[")
					.append(c)
					.append("],i))p.outsideCounts[")
					.append(c)
					.append("][i]=o; } else { int f=p.offsets[")
					.append(c)
					.append("][i],t=p.offsets[")
					.append(c)
					.append("][i+1]; long[] cs=p.selectors[")
					.append(c)
					.append("],co=p.outsideCounts[")
					.append(c)
					.append("]; for(int j=next(cs,f,t);j>=0&&j<t;j=next(cs,j+1,t))co[j]=o; }}\n");
		}
		s.append("    } }\n");
	}
}
