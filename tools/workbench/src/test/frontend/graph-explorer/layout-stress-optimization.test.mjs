/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
import assert from 'node:assert/strict';
import test from 'node:test';
import {BoundsTree} from '../../../main/frontend/graph-explorer/spatial-grid.ts';
import {stressLayout, untangle, drawingCost} from '../../../main/frontend/graph-explorer/graph-layout-stress.ts';
import {referenceUntangle} from './support/reference-untangle.ts';

function random(seed) {
  let state=seed;
  return ()=>{state^=state<<13;state^=state>>>17;state^=state<<5;return(state>>>0)/4294967296;};
}
const bytes = array => Buffer.from(array.buffer,array.byteOffset,array.byteLength);
function samePositions(a,b) {
  assert.ok(bytes(a.x).equals(bytes(b.x)), 'every Float64 x byte matches the scan oracle');
  assert.ok(bytes(a.y).equals(bytes(b.y)), 'every Float64 y byte matches the scan oracle');
}

test('bounds tree matches scans after refits and includes temporary movers once in original order', () => {
  const next=random(73),n=129;
  const minX=new Float64Array(n),minY=new Float64Array(n),maxX=new Float64Array(n),maxY=new Float64Array(n);
  for(let i=0;i<n;i++) {
    minX[i]=next()*4000-2000;minY[i]=next()*3000-1500;
    maxX[i]=minX[i]+next()*700;maxY[i]=minY[i]+next()*400;
  }
  const tree=new BoundsTree(minX,minY,maxX,maxY),found=[];
  for(let step=0;step<80;step++) {
    if(step%3===0) {
      const i=Math.floor(next()*n),dx=next()*3000-1500,dy=next()*2000-1000;
      minX[i]+=dx;maxX[i]+=dx;minY[i]+=dy;maxY[i]+=dy;
      tree.update(i,minX[i],minY[i],maxX[i],maxY[i]);
    }
    const lx=next()*6000-3000,ly=next()*4000-2000,hx=lx+next()*1700,hy=ly+next()*1200;
    const include=step%2?[step%n,(step*7)%n,step%n]:[];
    const expected=Array.from({length:n},(_,i)=>i).filter(i=>include.includes(i)||(maxX[i]>=lx&&minX[i]<=hx&&maxY[i]>=ly&&minY[i]<=hy));
    assert.deepEqual(tree.search(lx,ly,hx,hy,found,include),expected);
  }
  assert.deepEqual(new BoundsTree(new Float64Array(),new Float64Array(),new Float64Array(),new Float64Array()).search(0,0,10,10),[]);
});

function problem(n,seed,bodies=false) {
  const next=random(seed),links=[];
  for(let i=0;i<n;i++) {
    links.push(i,(i+1)%n);
    if(i%2===0)links.push(i,(i+5)%n);
    if(i%3===0)links.push(i,(i+17)%n);
  }
  return {
    x:Float64Array.from({length:n},()=>next()*1800-900),
    y:Float64Array.from({length:n},()=>next()*1000-500),
    left:Float64Array.from({length:n},(_,i)=>45+i%5*8),right:Float64Array.from({length:n},(_,i)=>50+i%7*6),
    top:Float64Array.from({length:n},(_,i)=>15+i%3*2),bottom:Float64Array.from({length:n},(_,i)=>17+i%4*3),
    links:Int32Array.from(links),fixed:Uint8Array.from({length:n},(_,i)=>i%11===0?1:0),
    bodies:bodies?Array.from({length:n},(_,i)=>i%9===1?[{minX:100,minY:-24,maxX:250,maxY:24},{minX:-90,minY:60,maxX:80,maxY:106}]:undefined):undefined,
  };
}

test('untangling exactly preserves candidate costs, fixed cards and asymmetric fan bodies', () => {
  for(const [n,seed,bodies,options] of [[12,41,false,{}],[72,91,false,{}],[80,123,true,{}],[64,711,true,{crossings:0,moveCost:0.3}]]) {
    const a=problem(n,seed,bodies),b=structuredClone(a),before=structuredClone(a);
    untangle(a,2,options);referenceUntangle(b,2,options);
    samePositions(a,b);
    assert.equal(drawingCost(a),drawingCost(b));
    for(let i=0;i<n;i++)if(a.fixed[i])assert.deepEqual([a.x[i],a.y[i]],[before.x[i],before.y[i]]);
  }
});

test('enabled tree queries preserve early-out crossing and card-cut scores when fan bodies move', () => {
  const a=problem(96,2211,true),links=[];
  for(let i=0;i<96;i++) {
    const group=Math.floor(i/8),start=group*8;
    a.x[i]=a.x[i]*0.18+group*1500;a.y[i]*=0.3;
    links.push(i,start+(i-start+1)%8,i,start+(i-start+3)%8);
    if(i===start&&i>0)links.push(i,i-8);
  }
  a.links=Int32Array.from(links);
  const b=structuredClone(a),before=structuredClone(a);
  let trialQueries=0;
  const search=BoundsTree.prototype.search;
  BoundsTree.prototype.search=function(...args) {
    if(args[5]?.length)trialQueries++;
    return search.apply(this,args);
  };
  try { untangle(a,2); } finally { BoundsTree.prototype.search=search; }
  referenceUntangle(b,2);
  assert.ok(trialQueries>0,'the real tree path, beyond its initial sample queries, was exercised');
  samePositions(a,b);
  assert.equal(drawingCost(a),drawingCost(b));
  assert.ok(a.bodies.some((boxes,i)=>boxes&&!a.fixed[i]&&(a.x[i]!==before.x[i]||a.y[i]!==before.y[i])), 'at least one fan owner moved');
});

test('temporary two-card swaps and accepted tree refits preserve the exact scan decision', () => {
  const n=40;
  const a={x:new Float64Array(n),y:new Float64Array(n),left:new Float64Array(n).fill(15),right:new Float64Array(n).fill(15),
    top:new Float64Array(n).fill(5),bottom:new Float64Array(n).fill(5),fixed:new Uint8Array(n).fill(1),links:null};
  a.x.set([-100,100,-100,100]);a.y.set([-50,50,50,-50]);a.fixed[1]=a.fixed[3]=0;
  const links=[0,1,2,3];
  for(let i=4;i<n;i++){a.x[i]=1000+i*150;a.y[i]=1000;if(i>4)links.push(i-1,i);}
  a.links=Int32Array.from(links);
  const b=structuredClone(a);
  untangle(a,3,{moveCost:1e6});referenceUntangle(b,3,{moveCost:1e6});
  samePositions(a,b);
  assert.equal(a.y[1],-50);assert.equal(a.y[3],50);
});

test('large drawing coordinates retain the scan fallback and packed indices reject truncation', () => {
  const a=problem(40,333,true),b=structuredClone(a);
  for(let i=0;i<a.x.length;i++){a.x[i]+=2**43;b.x[i]+=2**43;}
  untangle(a,1);referenceUntangle(b,1);samePositions(a,b);
  const empty=new Float64Array();
  assert.throws(()=>stressLayout({x:new Float64Array(65537),y:empty,left:empty,right:empty,top:empty,bottom:empty,links:new Int32Array()}),
    {name:'RangeError',message:'Stress layout supports at most 65536 nodes'});
});
