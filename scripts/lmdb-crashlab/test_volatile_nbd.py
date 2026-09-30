#!/usr/bin/env python3
# Adapted from the RDF4J CrashLab source archive; see the accompanying MIT LICENSE.
import errno
import importlib.util
import os
from pathlib import Path
import socket
import struct
import sys
import tempfile
import threading
import time
import unittest

spec=importlib.util.spec_from_file_location('volatile_nbd',Path(__file__).with_name('volatile_nbd.py'))
v=importlib.util.module_from_spec(spec);sys.modules[spec.name]=v;spec.loader.exec_module(v)

class DiskTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.path=Path(self.tmp.name)/'image.raw'
        with self.path.open('wb') as f:f.truncate(1024*1024)
        self.d=v.Disk(self.path)
    def tearDown(self):
        self.d.close();self.tmp.cleanup()
    def stable(self,off=0,n=512):
        with self.path.open('rb') as f:f.seek(off);return f.read(n)
    def test_write_ack_is_not_persistence(self):
        self.d.write(0,b'A'*512);self.assertEqual(self.d.read(0,512),b'A'*512)
        self.assertEqual(self.stable(),bytes(512));self.d.cut();self.d.reset()
        self.assertEqual(self.d.read(0,512),bytes(512))
    def test_flush_survives(self):
        self.d.write(0,b'A'*512);self.d.flush();self.d.cut();self.d.reset()
        self.assertEqual(self.d.read(0,512),b'A'*512)
    def test_fua_does_not_flush_unrelated_data(self):
        self.d.write(0,b'A'*512);self.d.write(512,b'B'*512,True);self.d.cut();self.d.reset()
        self.assertEqual(self.d.read(0,512),bytes(512));self.assertEqual(self.d.read(512,512),b'B'*512)
    def test_volatile_overwrite_preserves_old_stable(self):
        self.d.write(0,b'A'*512,True);self.d.write(0,b'B'*512);self.d.cut();self.d.reset()
        self.assertEqual(self.d.read(0,512),b'A'*512)
    def test_fua_overwrite_persists_new_value(self):
        self.d.write(0,b'A'*512);self.d.write(0,b'B'*512,True);self.d.cut();self.d.reset()
        self.assertEqual(self.d.read(0,512),b'B'*512)
    def test_random_sector_survival_is_deterministic(self):
        self.d.write(0,b'X'*(512*64));report=self.d.cut('random',.5,41)
        self.assertGreater(report['kept_sectors'],0);self.assertLess(report['kept_sectors'],64)
        actual=[i for i in range(0,512*64,512) if self.stable(i)==b'X'*512]
        self.assertEqual(actual,report['kept_offsets'])
        self.d.reset();self.d.write(0,b'Y'*(512*64));again=self.d.cut('random',.5,41)
        self.assertEqual(again['kept_offsets'],report['kept_offsets'])
    def test_page_can_tear_at_sector_boundaries(self):
        self.d.write(0,b'A'*4096,True);self.d.write(0,b'B'*4096)
        self.d.cut('random',.5,7);data=self.stable(0,4096)
        self.assertIn(b'A'*512,data);self.assertIn(b'B'*512,data)
        for i in range(0,4096,512):self.assertIn(data[i:i+512],(b'A'*512,b'B'*512))
    def test_all_survival(self):
        self.d.write(0,b'A'*512);self.d.cut('all');self.assertEqual(self.stable(),b'A'*512)
    def test_cut_fences_io(self):
        self.d.cut()
        for fn in (lambda:self.d.read(0,512),lambda:self.d.write(0,b'X'*512),self.d.flush):
            with self.assertRaises(v.PowerLost):fn()
    def test_incomplete_flush_can_be_cut(self):
        self.d.delay=.002;self.d.write(0,b'A'*(512*128));errors=[]
        def flush():
            try:self.d.flush()
            except Exception as e:errors.append(e)
        t=threading.Thread(target=flush);t.start()
        end=time.monotonic()+2
        while self.d.counts['destaged']==0 and time.monotonic()<end:time.sleep(.001)
        self.d.cut();t.join(3)
        self.assertFalse(t.is_alive());self.assertTrue(errors)
        self.assertEqual(self.d.counts['completed_flushes'],0)
        n=self.d.counts['destaged'];self.assertGreater(n,0);self.assertLess(n,128)
        self.assertEqual(self.stable(0,512*n),b'A'*(512*n))
        self.assertEqual(self.stable(512*n,512*(128-n)),bytes(512*(128-n)))
    def test_completed_flush_never_lost_in_later_cut(self):
        self.d.write(0,b'A'*4096);self.d.flush();self.d.write(4096,b'B'*4096)
        self.d.cut('random',.2,3);self.assertEqual(self.stable(0,4096),b'A'*4096)
    def test_lying_flush_negative_control(self):
        self.d.ignore_flush=True;self.d.write(0,b'A'*512);self.d.flush();self.d.cut();self.d.reset()
        self.assertNotEqual(self.d.read(0,512),b'A'*512)
    def test_alignment_and_bounds(self):
        for off,n in ((1,512),(0,1),(1024*1024,512),(-512,512)):
            with self.assertRaises(OSError):self.d.read(off,n)
    def test_exclusive_image_lock(self):
        with self.assertRaises(BlockingIOError):v.Disk(self.path)
    def test_reset_requires_cut(self):
        with self.assertRaises(ValueError):self.d.reset()
    def test_cache_cap_destages(self):
        self.d.max_sectors=2;self.d.write(0,b'A'*(512*3))
        self.assertEqual(len(self.d.dirty),2);self.assertEqual(self.d.counts['destaged'],1)
        self.d.cut();self.assertEqual(self.stable(),b'A'*512)

class WireTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();path=Path(self.tmp.name)/'image.raw'
        with path.open('wb') as f:f.truncate(1024*1024)
        self.d=v.Disk(path);self.srv=v.NBDServer(('127.0.0.1',0),self.d)
        self.thread=threading.Thread(target=self.srv.serve_forever);self.thread.start()
        self.s=socket.create_connection(self.srv.server_address);self.s.settimeout(2)
        self.assertEqual(struct.unpack('>QQH',v.recvn(self.s,18)),(v.NBD_MAGIC,v.OPT_MAGIC,3))
        self.s.sendall(struct.pack('>I',3));self.handle=0
    def tearDown(self):
        self.s.close();self.srv.disconnect_all();self.srv.shutdown();self.srv.server_close();self.thread.join()
        end=time.monotonic()+2
        while self.srv.client_lock.locked() and time.monotonic()<end:time.sleep(.001)
        self.d.close();self.tmp.cleanup()
    def option(self,opt,payload=b''):
        self.s.sendall(struct.pack('>QII',v.OPT_MAGIC,opt,len(payload))+payload)
    def read_option(self):
        magic,opt,typ,n=struct.unpack('>QIII',v.recvn(self.s,20));self.assertEqual(magic,v.REP_MAGIC)
        return opt,typ,v.recvn(self.s,n)
    def export(self):
        self.option(1,b'crashlab');size,flags=struct.unpack('>QH',v.recvn(self.s,10))
        self.assertEqual(size,1024*1024);self.assertEqual(flags,v.EXPORT_FLAGS)
    def command(self,cmd,offset=0,length=0,data=b'',flags=0):
        self.handle+=1;self.s.sendall(struct.pack('>IHHQQI',v.REQ_MAGIC,flags,cmd,self.handle,offset,length)+data)
        magic,error,handle=struct.unpack('>IIQ',v.recvn(self.s,16))
        self.assertEqual(magic,v.SIMPLE_MAGIC);self.assertEqual(handle,self.handle)
        return error,v.recvn(self.s,length) if cmd==0 and error==0 else b''
    def test_export_write_flush_read(self):
        self.export();self.assertEqual(self.command(1,0,512,b'Z'*512)[0],0)
        self.assertEqual(self.command(0,0,512),(0,b'Z'*512));self.assertEqual(self.command(3)[0],0)
        self.d.cut();self.d.reset();self.assertEqual(self.d.read(0,512),b'Z'*512)
    def test_fua_wire_flag(self):
        self.export();self.assertEqual(self.command(1,0,512,b'F'*512,1)[0],0)
        self.d.cut();self.d.reset();self.assertEqual(self.d.read(0,512),b'F'*512)
    def test_automatic_write_cut_never_sends_success_reply(self):
        self.export();self.d.arm("write",seed=41,request_window=1)
        with self.assertRaises(EOFError):self.command(1,0,4096,b'X'*4096)
        self.assertTrue(self.d.off);self.assertEqual(self.d.counts['cuts'],1)
    def test_automatic_flush_cut_never_sends_success_reply(self):
        self.export();self.command(1,0,4096,b'X'*4096)
        self.d.arm("persistence",seed=41,request_window=1)
        with self.assertRaises(EOFError):self.command(3)
        self.assertEqual(self.d.counts['completed_flushes'],0)
    def test_fua_wire_flag_does_not_rescue_other_volatile_write(self):
        self.export()
        self.assertEqual(self.command(1,0,512,b'A'*512)[0],0)
        self.assertEqual(self.command(1,512,512,b'B'*512,1)[0],0)
        self.d.cut();self.d.reset()
        self.assertEqual(self.command(0,0,512),(0,bytes(512)))
        self.assertEqual(self.command(0,512,512),(0,b'B'*512))
    def test_go_negotiation(self):
        name=b'crashlab';self.option(7,struct.pack('>I',len(name))+name+struct.pack('>HH',1,3))
        replies=[]
        while True:
            r=self.read_option();replies.append(r)
            if r[1]==1:break
        self.assertEqual(len(replies),3);self.assertEqual(self.command(0,0,512),(0,bytes(512)))
    def test_structured_reply_rejected_then_fallback(self):
        self.option(8);self.assertEqual(self.read_option()[1],0x80000001);self.export()
        self.assertEqual(self.command(3)[0],0)
    def test_out_of_bounds_errno(self):
        self.export();self.assertEqual(self.command(0,1024*1024,512)[0],errno.EINVAL)
    def test_unsupported_command_errno(self):
        self.export();self.assertEqual(self.command(6,0,512)[0],errno.EINVAL)
    def test_invalid_command_flags_rejected(self):
        self.export();self.assertEqual(self.command(1,0,512,b'X'*512,flags=2)[0],errno.EINVAL)
    def test_second_client_is_not_allowed(self):
        self.export()
        with socket.create_connection(self.srv.server_address) as second:
            second.settimeout(2);self.assertEqual(second.recv(1),b'')
    def test_disconnect_does_not_flush(self):
        self.export();self.command(1,0,512,b'Z'*512)
        self.s.sendall(struct.pack('>IHHQQI',v.REQ_MAGIC,0,2,99,0,0));self.s.close()
        time.sleep(.01);self.d.cut();self.d.reset();self.assertEqual(self.d.read(0,512),bytes(512))

if __name__=='__main__':unittest.main(verbosity=2)
