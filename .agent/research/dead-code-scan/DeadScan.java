import java.lang.classfile.*;
import java.lang.classfile.attribute.*;
import java.lang.classfile.constantpool.*;
import java.lang.classfile.instruction.*;
import java.lang.constant.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class DeadScan {
    record Ref(String owner, String name, String desc) {}
    static final class CI {
        String name, superName; List<String> ifaces = new ArrayList<>();
        boolean test, rec, en, iface; int flags;
        Map<String,Integer> methods = new LinkedHashMap<>(); // name+desc -> flags
        Map<String,Integer> fields = new LinkedHashMap<>();
        Map<String,String> fieldDesc = new HashMap<>();
        Set<String> constFields = new HashSet<>();
        Set<String> recComps = new HashSet<>();
        String srcFile;
    }
    static Map<String,CI> classes = new HashMap<>();
    static Map<Ref,Set<String>> mrefs = new HashMap<>();   // method ref -> referencing classes
    static Map<Ref,Set<String>> frefsR = new HashMap<>();  // field reads
    static Map<Ref,Set<String>> frefsW = new HashMap<>();  // field writes
    static Set<String> typeRefs = new HashSet<>();          // "refClass\towner" class usages
    static Map<String,Set<String>> typeRefBy = new HashMap<>();

    static void addRef(Map<Ref,Set<String>> m, Ref r, String from){ m.computeIfAbsent(r,k->new HashSet<>()).add(from); }
    static void typeRef(String type, String from){ if(type==null) return; typeRefBy.computeIfAbsent(type,k->new HashSet<>()).add(from); }
    static void typeRefDesc(String desc, String from){
        int i=0; while((i=desc.indexOf('L',i))>=0){ int j=desc.indexOf(';',i); if(j<0) break; typeRef(desc.substring(i+1,j),from); i=j+1; }
    }

    public static void main(String[] a) throws Exception {
        List<Path> roots = new ArrayList<>();
        for (String r : a[0].split(",")) roots.add(Path.of(r));
        String[] scope = a[1].split(",");
        String cp = a.length>2 ? a[2] : "";
        ClassFile cf = ClassFile.of();
        for (Path root : roots) {
            boolean test = root.toString().contains("test-classes");
            try (Stream<Path> s = Files.walk(root)) {
                for (Path p : (Iterable<Path>) s.filter(x->x.toString().endsWith(".class"))::iterator) {
                    ClassModel cm;
                    try { cm = cf.parse(Files.readAllBytes(p)); } catch (Exception e) { continue; }
                    CI ci = new CI();
                    ci.name = cm.thisClass().asInternalName();
                    ci.superName = cm.superclass().map(ClassEntry::asInternalName).orElse(null);
                    for (ClassEntry ce : cm.interfaces()) ci.ifaces.add(ce.asInternalName());
                    ci.test = test; ci.flags = cm.flags().flagsMask();
                    ci.iface = (ci.flags & ClassFile.ACC_INTERFACE)!=0;
                    ci.en = (ci.flags & ClassFile.ACC_ENUM)!=0;
                    ci.rec = cm.findAttribute(Attributes.record()).isPresent();
                    cm.findAttribute(Attributes.record()).ifPresent(ra -> { for (RecordComponentInfo rc : ra.components()) ci.recComps.add(rc.name().stringValue()); });
                    ci.srcFile = cm.findAttribute(Attributes.sourceFile()).map(sf->sf.sourceFile().stringValue()).orElse("");
                    typeRef(ci.superName, ci.name); for (String i : ci.ifaces) typeRef(i, ci.name);
                    for (FieldModel fm : cm.fields()) {
                        String fn = fm.fieldName().stringValue();
                        ci.fields.put(fn, fm.flags().flagsMask());
                        ci.fieldDesc.put(fn, fm.fieldType().stringValue());
                        typeRefDesc(fm.fieldType().stringValue(), ci.name);
                        if (fm.findAttribute(Attributes.constantValue()).isPresent()) ci.constFields.add(fn);
                    }
                    for (MethodModel mm : cm.methods()) {
                        String mn = mm.methodName().stringValue(), md = mm.methodType().stringValue();
                        ci.methods.put(mn+md, mm.flags().flagsMask());
                        typeRefDesc(md, ci.name);
                        mm.code().ifPresent(code -> {
                            for (CodeElement ce : code) {
                                if (ce instanceof InvokeInstruction ii) {
                                    addRef(mrefs, new Ref(ii.owner().asInternalName(), ii.name().stringValue(), ii.type().stringValue()), ci.name);
                                    typeRef(ii.owner().asInternalName(), ci.name);
                                } else if (ce instanceof InvokeDynamicInstruction idi) {
                                    for (ConstantDesc cd : idi.bootstrapArgs()) collectMH(cd, ci.name);
                                } else if (ce instanceof FieldInstruction fi) {
                                    Ref r = new Ref(fi.owner().asInternalName(), fi.name().stringValue(), fi.type().stringValue());
                                    Opcode op = fi.opcode();
                                    if (op==Opcode.GETFIELD||op==Opcode.GETSTATIC) addRef(frefsR, r, ci.name); else addRef(frefsW, r, ci.name);
                                    typeRef(fi.owner().asInternalName(), ci.name);
                                } else if (ce instanceof ConstantInstruction.LoadConstantInstruction lci) {
                                    if (lci.constantEntry() instanceof MethodHandleEntry mhe) {
                                        MemberRefEntry mr = mhe.reference();
                                        addRef(mrefs, new Ref(mr.owner().asInternalName(), mr.name().stringValue(), mr.type().stringValue()), ci.name);
                                    } else if (lci.constantEntry() instanceof ClassEntry cen) typeRef(cen.asInternalName(), ci.name);
                                } else if (ce instanceof NewObjectInstruction no) { typeRef(no.className().asInternalName(), ci.name);
                                } else if (ce instanceof TypeCheckInstruction tc) { typeRef(tc.type().asInternalName(), ci.name);
                                } else if (ce instanceof NewReferenceArrayInstruction na) { typeRef(na.componentType().asInternalName(), ci.name); }
                            }
                        });
                    }
                    classes.put(ci.name, ci);
                }
            }
        }
        // reflection loader for unscanned supertypes
        List<java.net.URL> urls = new ArrayList<>();
        if (!cp.isEmpty()) for (String e : cp.split(java.io.File.pathSeparator)) { try { urls.add(Path.of(e).toUri().toURL()); } catch (Exception ex) {} }
        ClassLoader loader = new java.net.URLClassLoader(urls.toArray(new java.net.URL[0]), ClassLoader.getSystemClassLoader());

        // resolve refs -> declaring scanned methods
        Set<String> liveAll = new HashSet<>(), liveMain = new HashSet<>();
        Map<String,Set<String>> refByAll = new HashMap<>(), refByMain = new HashMap<>();
        for (var e : mrefs.entrySet()) {
            Ref r = e.getKey();
            String decl = resolveMethod(r.owner(), r.name()+r.desc());
            if (decl == null) continue;
            String key = decl+"#"+r.name()+r.desc();
            for (String from : e.getValue()) {
                liveAll.add(key); refByAll.computeIfAbsent(key,k->new HashSet<>()).add(from);
                CI fc = classes.get(from);
                if (fc!=null && !fc.test) { liveMain.add(key); refByMain.computeIfAbsent(key,k->new HashSet<>()).add(from); }
            }
        }
        // override closure: a method overriding a live or unscanned method is live
        boolean changed = true;
        Set<String> overridesUnscanned = new HashSet<>();
        for (CI ci : classes.values()) if (inScope(ci.name, scope)) for (String m : ci.methods.keySet()) {
            String r = superDecl(ci, m, loader);
            if (r != null && r.equals("<unscanned>")) overridesUnscanned.add(ci.name+"#"+m);
        }
        while (changed) { changed=false;
            for (CI ci : classes.values()) for (String m : ci.methods.keySet()) {
                String key = ci.name+"#"+m;
                if (liveAll.contains(key) && liveMain.contains(key)) continue;
                String sd = superDecl(ci, m, loader);
                if (sd==null) continue;
                if (sd.equals("<unscanned>")) { if (liveAll.add(key)) changed=true; if (liveMain.add(key)) changed=true; continue; }
                String sk = sd+"#"+m;
                if (liveAll.contains(sk) && liveAll.add(key)) changed=true;
                if (liveMain.contains(sk) && liveMain.add(key)) changed=true;
            }
        }
        // overridden-by: mark that a method has overriders
        Map<String,Integer> overriderCount = new HashMap<>(); Map<String,List<String>> overriderKeys = new HashMap<>();
        for (CI ci : classes.values()) for (String m : ci.methods.keySet()) { String sd = superDecl(ci, m, loader); if (sd!=null && !sd.equals("<unscanned>")) { overriderCount.merge(sd+"#"+m,1,Integer::sum); overriderKeys.computeIfAbsent(sd+"#"+m,k->new ArrayList<>()).add(ci.name); } }

        StringBuilder out = new StringBuilder();
        out.append("KIND\tCLASS\tMEMBER\tFLAGS\tSRC\tNOTE\n");
        for (CI ci : classes.values().stream().sorted(Comparator.comparing(c->c.name)).toList()) {
            if (!inScope(ci.name, scope) || ci.test) continue;
            for (var me : ci.methods.entrySet()) {
                String m = me.getKey(); int fl = me.getValue();
                if ((fl & ClassFile.ACC_SYNTHETIC)!=0 || (fl & ClassFile.ACC_BRIDGE)!=0) continue;
                String name = m.substring(0, m.indexOf('('));
                if (name.equals("<clinit>")) continue;
                if (ci.en && (name.equals("values")||name.equals("valueOf")||name.equals("$values"))) continue;
                if (ci.rec && (ci.recComps.contains(name)||name.equals("toString")||name.equals("hashCode")||name.equals("equals"))) continue;
                if (name.equals("main")||name.equals("readObject")||name.equals("writeObject")||name.equals("readResolve")||name.equals("writeReplace")) continue;
                String key = ci.name+"#"+m;
                boolean la = liveAll.contains(key), lm = liveMain.contains(key);
                if (la && lm) continue;
                String kind = !la ? (name.equals("<init>") ? "DEAD-CTOR" : "DEAD-METHOD") : (name.equals("<init>") ? "TESTONLY-CTOR" : "TESTONLY-METHOD");
                String note = "";
                int oc = overriderCount.getOrDefault(key,0); if (oc>0) note += "overriders="+oc+":"+String.join("|",overriderKeys.get(key))+";";
                if (la && !lm) note += "testrefs="+refByAll.getOrDefault(key,Set.of()).stream().sorted().limit(3).collect(Collectors.joining(","))+";";
                out.append(kind).append('\t').append(ci.name).append('\t').append(m).append('\t').append(flagStr(fl)).append('\t').append(ci.srcFile).append('\t').append(note).append('\n');
            }
            for (var fe : ci.fields.entrySet()) {
                String f = fe.getKey(); int fl = fe.getValue();
                if ((fl & ClassFile.ACC_SYNTHETIC)!=0) continue;
                if (ci.en && (fl & ClassFile.ACC_ENUM)!=0) continue;
                if (ci.rec && ci.recComps.contains(f)) continue;
                if (f.equals("serialVersionUID")) continue;
                Set<String> rAll = new HashSet<>(), rMain = new HashSet<>(), wAll = new HashSet<>(), wMain = new HashSet<>();
                collectField(ci, f, frefsR, rAll, rMain); collectField(ci, f, frefsW, wAll, wMain);
                String kind = null, note = "";
                boolean isConst = ci.constFields.contains(f);
                if (rAll.isEmpty() && wAll.isEmpty()) kind = isConst ? "UNREF-CONST(check-src)" : "DEAD-FIELD";
                else if (rAll.isEmpty()) kind = isConst ? "UNREF-CONST(check-src)" : "WRITEONLY-FIELD";
                else if (rMain.isEmpty()) { kind = "TESTONLY-FIELD"; note = "testrefs="+rAll.stream().sorted().limit(3).collect(Collectors.joining(","))+";"; }
                if (kind!=null) out.append(kind).append('\t').append(ci.name).append('\t').append(f).append(' ').append(ci.fieldDesc.get(f)).append('\t').append(flagStr(fl)).append('\t').append(ci.srcFile).append('\t').append(note).append('\n');
            }
        }
        // class-level: scoped main classes never referenced by anyone else (main or test)
        for (CI ci : classes.values().stream().sorted(Comparator.comparing(c->c.name)).toList()) {
            if (!inScope(ci.name, scope) || ci.test) continue;
            Set<String> by = new HashSet<>(typeRefBy.getOrDefault(ci.name, Set.of()));
            by.remove(ci.name);
            String outer = ci.name.contains("$") ? ci.name.substring(0, ci.name.indexOf('$')) : null;
            Set<String> byMain = by.stream().filter(b -> { CI c = classes.get(b); return c==null || !c.test; }).collect(Collectors.toSet());
            Set<String> byOther = by.stream().filter(b -> outer==null || !(b.equals(outer)||b.startsWith(outer+"$"))).collect(Collectors.toSet());
            if (by.isEmpty()) out.append("DEAD-CLASS\t").append(ci.name).append("\t-\t").append(flagStr(ci.flags)).append('\t').append(ci.srcFile).append("\t\n");
            else if (byMain.isEmpty()) out.append("TESTONLY-CLASS\t").append(ci.name).append("\t-\t").append(flagStr(ci.flags)).append('\t').append(ci.srcFile).append("\ttestrefs=").append(by.stream().sorted().limit(3).collect(Collectors.joining(","))).append("\n");
            else if (byOther.isEmpty() && outer!=null) out.append("OUTER-ONLY-CLASS\t").append(ci.name).append("\t-\t").append(flagStr(ci.flags)).append('\t').append(ci.srcFile).append("\t\n");
        }
        System.out.print(out);
    }
    static void collectField(CI ci, String f, Map<Ref,Set<String>> refs, Set<String> all, Set<String> main) {
        // refs whose owner is ci or a subclass of ci and resolve to ci.f
        for (var e : refs.entrySet()) {
            Ref r = e.getKey(); if (!r.name().equals(f)) continue;
            String decl = resolveField(r.owner(), f);
            if (!ci.name.equals(decl)) continue;
            for (String from : e.getValue()) { all.add(from); CI fc = classes.get(from); if (fc==null || !fc.test) main.add(from); }
        }
    }
    static String resolveField(String owner, String f) {
        CI c = classes.get(owner); if (c==null) return null;
        if (c.fields.containsKey(f)) return c.name;
        for (String i : c.ifaces) { String r = resolveField(i, f); if (r!=null) return r; }
        return c.superName==null?null:resolveField(c.superName, f);
    }
    static void collectMH(ConstantDesc cd, String from) {
        if (cd instanceof DirectMethodHandleDesc d) {
            String o = d.owner().descriptorString(); o = o.substring(1, o.length()-1);
            addRef(mrefs, new Ref(o, d.methodName(), d.lookupDescriptor()), from);
            typeRef(o, from);
        } else if (cd instanceof DynamicConstantDesc<?> dd) { for (ConstantDesc x : dd.bootstrapArgsList()) collectMH(x, from); }
    }
    static String resolveMethod(String owner, String md) {
        CI c = classes.get(owner); if (c==null) return null;
        if (c.methods.containsKey(md)) return c.name;
        if (c.superName!=null) { String r = resolveMethod(c.superName, md); if (r!=null) return r; }
        for (String i : c.ifaces) { String r = resolveMethod(i, md); if (r!=null) return r; }
        return null;
    }
    static Map<String,String> superDeclCache = new HashMap<>();
    // returns declaring supertype name of an overridden method, "<unscanned>" if an unscanned supertype declares it, or null
    static String superDecl(CI ci, String md, ClassLoader loader) {
        String key = ci.name+"#"+md;
        if (superDeclCache.containsKey(key)) return superDeclCache.get(key);
        String r = null;
        if (!md.startsWith("<init>")) {
            List<String> sups = new ArrayList<>(); if (ci.superName!=null) sups.add(ci.superName); sups.addAll(ci.ifaces);
            for (String s : sups) { r = findDecl(s, md, loader); if (r!=null) break; }
        }
        superDeclCache.put(key, r); return r;
    }
    static Map<String,Set<String>> unscannedMethods = new HashMap<>();
    static String findDecl(String type, String md, ClassLoader loader) {
        CI c = classes.get(type);
        if (c==null) {
            Set<String> ms = unscannedMethods.computeIfAbsent(type, t -> {
                Set<String> set = new HashSet<>();
                try { Class<?> k = Class.forName(t.replace('/','.'), false, loader); collectAll(k, set); } catch (Throwable e) { set.add("<ALL>"); }
                return set; });
            if (ms.contains("<ALL>")) return "<unscanned>";
            return ms.contains(md) ? "<unscanned>" : null;
        }
        if (c.methods.containsKey(md)) return c.name;
        List<String> sups = new ArrayList<>(); if (c.superName!=null) sups.add(c.superName); sups.addAll(c.ifaces);
        for (String s : sups) { String r = findDecl(s, md, loader); if (r!=null) return r; }
        return null;
    }
    static void collectAll(Class<?> k, Set<String> set) {
        if (k==null) return;
        try { for (Method m : k.getDeclaredMethods()) set.add(m.getName()+java.lang.invoke.MethodType.methodType(m.getReturnType(), m.getParameterTypes()).descriptorString()); } catch (Throwable e) { set.add("<ALL>"); }
        collectAll(k.getSuperclass(), set); for (Class<?> i : k.getInterfaces()) collectAll(i, set);
    }
    static boolean inScope(String n, String[] scope){ for (String s : scope) if (n.startsWith(s)) return true; return false; }
    static String flagStr(int f){ StringBuilder b=new StringBuilder(); if((f&1)!=0)b.append("public "); if((f&2)!=0)b.append("private "); if((f&4)!=0)b.append("protected "); if((f&8)!=0)b.append("static "); if((f&0x10)!=0)b.append("final "); if((f&0x400)!=0)b.append("abstract "); return b.toString().trim(); }
}
