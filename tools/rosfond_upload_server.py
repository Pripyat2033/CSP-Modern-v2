#!/usr/bin/env python3
"""Drag-and-drop upload page: hand ROSFOND files to the agent with no GitHub.

Run it, open the printed link, drop the files (or the whole folder) into the page.
Nothing to install, no account, no zip, no release asset.

    python3 tools/rosfond_upload_server.py --port 12000 --out /workspace/rosfond_uploads

Endpoints:
    GET  /            the upload page
    PUT  /put/<name>  store one file (raw request body) -- used by the page
    GET  /status      JSON: how many files arrived and which of the 28 needed are present
    GET  /done        plain confirmation

It is upload-only: it never serves the stored files back out. Filenames are
reduced to their base name, so a dropped folder is flattened safely.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

# The 28 nuclides the lattice cell needs today (label -> the ZA the ENDF head carries).
NEEDED = {
    "H1": 1001, "He4": 2004, "B10": 5010, "B11": 5011, "C12": 6000, "C13": 6013,
    "N14": 7014, "O16": 8016, "Cr52": 24052, "Fe56": 26056, "Ni58": 28058,
    "Zr90": 40090, "Zr91": 40091, "Zr92": 40092, "Zr94": 40094, "Zr96": 40096,
    "Nb93": 41093, "Gd155": 64155, "Gd157": 64157, "Er166": 68166, "Er167": 68167,
    "U234": 92234, "U235": 92235, "U236": 92236, "U238": 92238,
    "Pu239": 94239, "Pu240": 94240, "Pu241": 94241,
}

PAGE = """<!doctype html><html><head><meta charset="utf-8">
<title>ROSFOND upload</title>
<style>
 body{font-family:system-ui,Arial,sans-serif;background:#0f1115;color:#e6e6e6;margin:0;padding:32px}
 h1{font-size:20px;margin:0 0 6px} p{color:#9aa4b2;margin:4px 0 18px}
 #drop{border:2px dashed #3b4252;border-radius:12px;padding:48px;text-align:center;background:#151922}
 #drop.hot{border-color:#5b8def;background:#182033}
 button{background:#5b8def;color:#fff;border:0;border-radius:8px;padding:10px 16px;font-size:14px;cursor:pointer;margin:6px}
 button.alt{background:#2b3446}
 #bar{height:10px;background:#2b3446;border-radius:6px;overflow:hidden;margin:18px 0 8px}
 #barfill{height:100%;width:0;background:#5b8def;transition:width .2s}
 #log{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:12px;color:#9aa4b2;
      max-height:240px;overflow:auto;background:#0b0e13;border-radius:8px;padding:12px;margin-top:12px}
 .ok{color:#5ad19b}.bad{color:#e06c75}
 #count{font-size:15px;margin-top:10px}
</style></head><body>
<h1>Drop your ROSFOND files here</h1>
<p>Select the whole folder (or the 28 files). No zip needed. Nothing leaves this page except into the agent's workspace.</p>
<div id="drop">Drag files or the folder here<br><br>
  <button onclick="f.click()">Choose files</button>
  <button class="alt" onclick="d.click()">Choose folder</button>
  <input id="f" type="file" multiple hidden>
  <input id="d" type="file" webkitdirectory hidden>
</div>
<div id="bar"><div id="barfill"></div></div>
<div id="count">waiting…</div>
<div id="log"></div>
<script>
const drop=document.getElementById('drop'),log=document.getElementById('log');
const barfill=document.getElementById('barfill'),count=document.getElementById('count');
const f=document.getElementById('f'),d=document.getElementById('d');
function say(s,cls){const l=document.createElement('div');if(cls)l.className=cls;l.textContent=s;log.appendChild(l);log.scrollTop=log.scrollHeight;}
drop.addEventListener('dragover',e=>{e.preventDefault();drop.classList.add('hot');});
drop.addEventListener('dragleave',()=>drop.classList.remove('hot'));
drop.addEventListener('drop',async e=>{e.preventDefault();drop.classList.remove('hot');
  let files=[];const items=e.dataTransfer.items;
  if(items&&items.length&&items[0].webkitGetAsEntry){const entries=[];
    for(const it of items){const en=it.webkitGetAsEntry&&it.webkitGetAsEntry();if(en)entries.push(en);}
    for(const en of entries){await walk(en,files);}
  } else {for(const fl of e.dataTransfer.files)files.push(fl);}
  upload(files);
});
f.addEventListener('change',()=>upload([...f.files]));
d.addEventListener('change',()=>upload([...d.files]));
async function walk(entry,out){if(entry.isFile){await new Promise(r=>entry.file(fl=>{out.push(fl);r();}));}
  else if(entry.isDirectory){const rd=entry.createReader();
    // readEntries() returns entries in BATCHES (typically ~100 per call); it must
    // be called repeatedly until it yields an empty batch. A single call silently
    // drops every file past the first batch — fatal for a 686-file folder.
    while(true){
      const ents=await new Promise(r=>rd.readEntries(r,()=>r([])));
      if(!ents.length)break;
      for(const e of ents)await walk(e,out);
    }}}
async function upload(files){
  if(!files.length){say('no files selected',"bad");return;}
  say('uploading '+files.length+' file(s)…');
  let done=0,failed=0,i=0;
  const total=files.length;
  // Upload a few at a time so 686 files do not crawl one-by-one, but not so many
  // that a slow link stalls everything.
  async function worker(){
    while(i<total){
      const fl=files[i++];
      const name=fl.name;
      try{
        const r=await fetch('/put/'+encodeURIComponent(name),{method:'PUT',body:fl});
        if(r.ok){done++;if(total<=40)say('  ok  '+name,"ok");}
        else{failed++;say('  FAIL '+name+' ('+r.status+')',"bad");}
      }catch(err){failed++;say('  ERR  '+name+' '+err,"bad");}
      barfill.style.width=(100*(done+failed)/total)+'%';
      if(total>40)count.textContent=(done+failed)+'/'+total+' sent ('+failed+' failed)…';
    }
  }
  await Promise.all([0,1,2,3].map(worker));
  say('done: '+done+'/'+total+' sent'+(failed?', '+failed+' failed':''),failed?"bad":"ok");
  refresh();
}
async function refresh(){
  try{const s=await (await fetch('/status')).json();
    count.innerHTML='<b>'+s.files+'</b> files received &middot; <b>'+s.present.length+'/'+s.needed+'</b> of the 28 needed present';
    if(s.present.length===s.needed)count.innerHTML+=' <span class="ok">&nbsp;ALL 28 HERE</span>';
  }catch(e){}
}
refresh();setInterval(refresh,3000);
</script></body></html>
"""


def _safe(name: str) -> str:
    name = os.path.basename(name.replace("\\", "/"))
    return re.sub(r"[^A-Za-z0-9._-]", "_", name)[:120] or "file"


class Handler(BaseHTTPRequestHandler):
    out = "."
    _present_cache: set[str] = set()
    _present_key = None

    def _send(self, code, body, ctype="text/plain"):
        b = body.encode() if isinstance(body, str) else body
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(b)))
        self.end_headers()
        self.wfile.write(b)

    def do_GET(self):
        if self.path == "/" or self.path.startswith("/?"):
            self._send(200, PAGE, "text/html; charset=utf-8")
        elif self.path == "/status":
            present = self._present()
            self._send(200, json.dumps({
                "files": len(os.listdir(self.out)) if os.path.isdir(self.out) else 0,
                "needed": len(NEEDED), "present": sorted(present),
                "missing": sorted(set(NEEDED) - set(present)),
            }), "application/json")
        elif self.path == "/done":
            self._send(200, "ok\n")
        else:
            self._send(404, "not found\n")

    def do_PUT(self):
        if not self.path.startswith("/put/"):
            self._send(404, "not found\n")
            return
        name = _safe(self.path[len("/put/"):])
        n = int(self.headers.get("Content-Length", 0))
        os.makedirs(self.out, exist_ok=True)
        with open(os.path.join(self.out, name), "wb") as fh:
            left = n
            while left > 0:
                chunk = self.rfile.read(min(1 << 20, left))
                if not chunk:
                    break
                fh.write(chunk)
                left -= len(chunk)
        self._send(200, f"stored {name} ({n} bytes)\n")

    def _present(self) -> set[str]:
        """Which needed nuclides are present, by reading the ENDF head record.

        Reading 686 files (some tens of MB) on every 3-second status poll would be
        wasteful, so cache the result and only re-scan when the directory listing
        changes. The head record is the first MF=1/MT=451 line, so only the start
        of each file is read.
        """
        found: set[str] = set()
        if not os.path.isdir(self.out):
            return found
        try:
            names = tuple(sorted(os.listdir(self.out)))
        except OSError:
            return found
        if names == self._present_key:
            return self._present_cache
        za_to_label = {za: lab for lab, za in NEEDED.items()}
        for name in names:
            path = os.path.join(self.out, name)
            if not os.path.isfile(path):
                continue
            try:
                with open(path, errors="replace") as fh:
                    for _ in range(2000):  # the head record is near the top
                        line = fh.readline()
                        if not line:
                            break
                        if len(line) >= 75 and line[70:72].strip() == "1" and line[72:75].strip() == "451":
                            field = line[:11].strip()
                            s = field
                            for i in range(len(s) - 1, 0, -1):
                                if s[i] in "+-":
                                    s = s[:i] + "e" + s[i:]
                                    break
                            try:
                                za = int(round(float(s)))
                            except ValueError:
                                break
                            lab = za_to_label.get(za)
                            if lab:
                                found.add(lab)
                            break
            except Exception:
                continue
        self._present_cache = found
        self._present_key = names
        return found

    def log_message(self, fmt, *args):
        print(f"  {self.address_string()} {fmt % args}")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--port", type=int, default=12000)
    ap.add_argument("--out", default="/workspace/rosfond_uploads")
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)
    Handler.out = args.out
    print(f"serving on 0.0.0.0:{args.port}  ->  {args.out}")
    ThreadingHTTPServer(("0.0.0.0", args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
