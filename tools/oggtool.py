#!/usr/bin/env python3
"""Dependency-free Ogg utility for SleepSlide bundled sounds.

    oggtool.py info  <file.ogg>...
    oggtool.py clean <in.ogg> <out.ogg> [--max-seconds N]

`clean` keeps only the primary audio logical stream (drops Ogg Skeleton
`fishead`/`fisbone` pages, which ExoPlayer's OggExtractor does not sniff) and
optionally truncates the stream at the first page whose granule position
reaches N seconds, setting the EOS flag and recomputing that page's CRC.
Pages are copied byte-for-byte, so this is lossless — no re-encoding.
"""
import struct
import sys

# Ogg page CRC: polynomial 0x04c11db7, no reflection, init 0, no final xor.
_CRC_TABLE = []
for _i in range(256):
    _r = _i << 24
    for _ in range(8):
        _r = ((_r << 1) ^ 0x04C11DB7) if _r & 0x80000000 else (_r << 1)
    _CRC_TABLE.append(_r & 0xFFFFFFFF)


def ogg_crc(data: bytes) -> int:
    crc = 0
    for b in data:
        crc = ((crc << 8) & 0xFFFFFFFF) ^ _CRC_TABLE[((crc >> 24) & 0xFF) ^ b]
    return crc


class Page:
    __slots__ = ("header_type", "granule", "serial", "seq", "segments", "body", "raw")

    def __init__(self, header_type, granule, serial, seq, segments, body, raw):
        self.header_type = header_type
        self.granule = granule
        self.serial = serial
        self.seq = seq
        self.segments = segments
        self.body = body
        self.raw = raw

    @property
    def bos(self):
        return bool(self.header_type & 0x02)

    @property
    def eos(self):
        return bool(self.header_type & 0x04)

    def with_eos(self) -> bytes:
        """Return page bytes with the EOS flag set and CRC recomputed."""
        header = bytearray(self.raw[: 27 + len(self.segments)])
        header[5] |= 0x04
        header[22:26] = b"\x00\x00\x00\x00"
        page = bytes(header) + self.body
        crc = ogg_crc(page)
        return page[:22] + struct.pack("<I", crc) + page[26:]


def read_pages(data: bytes):
    pos = 0
    n = len(data)
    while pos + 27 <= n:
        if data[pos : pos + 4] != b"OggS":
            nxt = data.find(b"OggS", pos + 1)
            if nxt < 0:
                return
            pos = nxt
            continue
        header_type = data[pos + 5]
        granule = struct.unpack_from("<q", data, pos + 6)[0]
        serial = struct.unpack_from("<I", data, pos + 14)[0]
        seq = struct.unpack_from("<I", data, pos + 18)[0]
        nseg = data[pos + 26]
        segments = data[pos + 27 : pos + 27 + nseg]
        body_len = sum(segments)
        start = pos + 27 + nseg
        end = start + body_len
        if end > n:
            return
        yield Page(header_type, granule, serial, seq, segments, data[start:end], data[pos:end])
        pos = end


def identify(bos_body: bytes):
    """Return (codec, sample_rate, channels) from a BOS packet, or (None, 0, 0)."""
    if bos_body.startswith(b"\x01vorbis"):
        channels = bos_body[11]
        rate = struct.unpack_from("<I", bos_body, 12)[0]
        return "vorbis", rate, channels
    if bos_body.startswith(b"OpusHead"):
        channels = bos_body[9]
        return "opus", 48000, channels  # Opus granules are always at 48 kHz
    if bos_body.startswith(b"\x7fFLAC"):
        # 0x7f 'FLAC' major minor nhdr(2) 'fLaC' then STREAMINFO block: 4 hdr + 34 body
        si = bos_body[13 + 4 :]
        rate = (si[10] << 12) | (si[11] << 4) | (si[12] >> 4)
        channels = ((si[12] >> 1) & 0x07) + 1
        return "flac", rate, channels
    if bos_body.startswith(b"fishead"):
        return "skeleton", 0, 0
    return None, 0, 0


def analyse(data: bytes):
    streams = {}
    for p in read_pages(data):
        s = streams.setdefault(p.serial, {"codec": None, "rate": 0, "channels": 0, "pages": 0, "last_granule": 0, "bytes": 0})
        if p.bos:
            s["codec"], s["rate"], s["channels"] = identify(p.body)
        s["pages"] += 1
        s["bytes"] += len(p.raw)
        if p.granule >= 0:
            s["last_granule"] = max(s["last_granule"], p.granule)
    return streams


def primary_audio(streams):
    for serial, s in streams.items():
        if s["codec"] in ("vorbis", "opus", "flac"):
            return serial, s
    return None, None


def cmd_info(paths):
    for path in paths:
        data = open(path, "rb").read()
        streams = analyse(data)
        serial, s = primary_audio(streams)
        others = [v["codec"] or "?" for k, v in streams.items() if k != serial]
        if s is None:
            print(f"{path}: NO AUDIO STREAM ({others})")
            continue
        dur = s["last_granule"] / s["rate"] if s["rate"] else 0
        kbps = s["bytes"] * 8 / dur / 1000 if dur else 0
        extra = f"  extra streams: {others}" if others else ""
        print(f"{path}\n    {s['codec']} {s['rate']} Hz {s['channels']}ch  {dur:8.1f}s  {len(data)/1e6:6.2f} MB  ~{kbps:.0f} kbps{extra}")


def cmd_clean(src, dst, max_seconds=None):
    data = open(src, "rb").read()
    streams = analyse(data)
    serial, s = primary_audio(streams)
    if s is None:
        sys.exit(f"{src}: no audio stream found")
    limit = int(max_seconds * s["rate"]) if max_seconds else None
    out = bytearray()
    kept = 0
    for p in read_pages(data):
        if p.serial != serial:
            continue
        if limit is not None and p.granule >= 0 and p.granule >= limit and not p.bos:
            out += p.with_eos()
            kept += 1
            break
        out += p.raw
        kept += 1
    open(dst, "wb").write(out)
    new = analyse(bytes(out))[serial]
    print(f"{src} -> {dst}: {kept}/{s['pages']} pages, {new['last_granule']/new['rate']:.1f}s, {len(out)/1e6:.2f} MB")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    if sys.argv[1] == "info":
        cmd_info(sys.argv[2:])
    elif sys.argv[1] == "clean":
        secs = None
        args = sys.argv[2:]
        if "--max-seconds" in args:
            i = args.index("--max-seconds")
            secs = float(args[i + 1])
            del args[i : i + 2]
        cmd_clean(args[0], args[1], secs)
    else:
        sys.exit(__doc__)
