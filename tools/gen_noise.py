#!/usr/bin/env python3
"""Generate seamless-loop noise colours as 16-bit mono WAV — no dependencies.

    gen_noise.py <out_dir> [--seconds 15] [--rate 44100]

Produces white.wav, pink.wav, brown.wav, grey.wav. Each loop is crossfaded
into itself over the last 0.5 s so REPEAT_MODE_ONE joins without a click,
then RMS-normalised to -20 dBFS with a -1 dBFS peak ceiling so three mixed
layers sit at the same level.

Pink  = Paul Kellet's 7-pole approximation (-3 dB/oct).
Brown = leaky integrator of white (-6 dB/oct).
Grey  = white re-weighted by a rough inverse-A curve (bass and treble lifted
        relative to 1-4 kHz) so it *sounds* flat — an approximation, not a
        calibrated psychoacoustic curve.
"""
import math
import random
import struct
import sys
import wave


def white(n, rng):
    return [rng.gauss(0.0, 1.0) for _ in range(n)]


def pink(n, rng):
    b0 = b1 = b2 = b3 = b4 = b5 = b6 = 0.0
    out = []
    for _ in range(n):
        w = rng.gauss(0.0, 1.0)
        b0 = 0.99886 * b0 + w * 0.0555179
        b1 = 0.99332 * b1 + w * 0.0750759
        b2 = 0.96900 * b2 + w * 0.1538520
        b3 = 0.86650 * b3 + w * 0.3104856
        b4 = 0.55000 * b4 + w * 0.5329522
        b5 = -0.7616 * b5 - w * 0.0168980
        out.append((b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362) * 0.11)
        b6 = w * 0.115926
    return out


def brown(n, rng):
    out = []
    acc = 0.0
    for _ in range(n):
        acc = 0.998 * acc + rng.gauss(0.0, 1.0) * 0.02  # leaky so it never wanders off
        out.append(acc)
    return out


def _one_pole_lp(x, rate, fc):
    a = math.exp(-2.0 * math.pi * fc / rate)
    y, out = 0.0, []
    for v in x:
        y = a * y + (1.0 - a) * v
        out.append(y)
    return out


def grey(n, rng, rate):
    w = white(n, rng)
    low = _one_pole_lp(w, rate, 250.0)                 # bass lift
    mid = _one_pole_lp(w, rate, 4000.0)
    high = [wi - mi for wi, mi in zip(w, mid)]          # treble above ~4 kHz
    return [0.35 * wi + 4.0 * lo + 0.9 * hi for wi, lo, hi in zip(w, low, high)]


def loopify(x, rate, xfade_s=0.5):
    """Crossfade the tail into the head (equal-power) and drop the tail."""
    k = int(rate * xfade_s)
    body = x[: len(x) - k]
    tail = x[len(x) - k :]
    for i in range(k):
        t = i / k
        fin, fout = math.sin(t * math.pi / 2), math.cos(t * math.pi / 2)
        body[i] = body[i] * fin + tail[i] * fout
    return body


def normalise(x, rms_db=-20.0, peak_db=-1.0):
    rms = math.sqrt(sum(v * v for v in x) / len(x)) or 1.0
    g = 10 ** (rms_db / 20) / rms
    peak = max(abs(v) for v in x) * g
    ceiling = 10 ** (peak_db / 20)
    if peak > ceiling:
        g *= ceiling / peak
    return [v * g for v in x]


def write_wav(path, x, rate):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1.0, min(1.0, v)) * 32767)) for v in x))


def main():
    out_dir = sys.argv[1]
    secs, rate = 15.0, 44100
    args = sys.argv[2:]
    if "--seconds" in args:
        secs = float(args[args.index("--seconds") + 1])
    if "--rate" in args:
        rate = int(args[args.index("--rate") + 1])
    n = int(secs * rate)
    for name, fn in (
        ("white", lambda r: white(n, r)),
        ("pink", lambda r: pink(n, r)),
        ("brown", lambda r: brown(n, r)),
        ("grey", lambda r: grey(n, r, rate)),
    ):
        rng = random.Random(f"sleepslide-{name}")  # reproducible builds
        x = normalise(loopify(fn(rng), rate))
        path = f"{out_dir}/{name}.wav"
        write_wav(path, x, rate)
        print(f"{path}: {len(x)/rate:.1f}s {rate} Hz mono, {len(x)*2/1e6:.2f} MB")


if __name__ == "__main__":
    main()
