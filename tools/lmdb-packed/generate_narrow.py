#!/usr/bin/env python3
"""Print the independently authored, rejected experimental native unpackers (1..8 bits).
Each eight-lane group starts width bytes after the previous group. The existing eight-byte
readable tail covers an unaligned final load. No dependency, vector module, or format change.
Redirect output for review; this script never modifies a source tree automatically.
"""

def generate():
 text='''\t/**
	 * Specialize only narrow lanes: eight values need one load, and constant shifts avoid the
	 * register pressure of the variable-width unrolled experiment. Wider widths keep the rolling reader.
	 * Bodies below are reproduced by tools/lmdb-packed/generate_narrow.py.
	 */
	private static void unpackNarrow(long payload, int fromLane, int length, int width, long base, int idShift,
			long[] target, int targetOffset) {
		switch (width) {
'''
 for w in range(1,9):
  text+=f'\t\tcase {w} -> unpack{w}(payload, fromLane, length, base, idShift, target, targetOffset);\n'
 text+='''		default -> throw new IllegalArgumentException("not a narrow packed width: " + width);
		}
	}

'''
 for w in range(1,9):
  mask=(1<<w)-1
  text+=f'''	private static void unpack{w}(long payload, int fromLane, int length, long base, int idShift,
			long[] target, int targetOffset) {{
		int bit = fromLane * {w};
		int end = targetOffset + length;
		int output = targetOffset;
		for (; output <= end - 8; output += 8, bit += {w*8}) {{
			long word = UnsafeAccess.getLongLE(payload + (bit >>> 3)) >>> (bit & 7);
'''
  for j in range(8):
   shift='word' if j==0 else f'(word >>> {j*w})'
   text+=f'\t\t\ttarget[output{f" + {j}" if j else ""}] = base + (({shift} & {mask}L) << idShift);\n'
  text+=f'''		}}
		for (; output < end; output++, bit += {w}) {{
			long value = UnsafeAccess.getLongLE(payload + (bit >>> 3)) >>> (bit & 7);
			target[output] = base + ((value & {mask}L) << idShift);
		}}
	}}

'''
 return text
if __name__=='__main__':print(generate(),end='')
