"""Encrogram für den PC – zum Testen per Copy & Paste.

Kompatibel mit dem Format der App (ENCROGRAM.md, Kapitel 4):
TENCDEC:1:<base64url( salt[16] ‖ nonce[12] ‖ geheimtext ‖ tag[16] )>

Benötigt:  python -m pip install argon2-cffi cryptography
Aufruf:    python tools/encrogram.py
"""

import base64
import datetime
import getpass
import os
import re
import struct
import sys
import unicodedata

from argon2.low_level import Type, hash_secret_raw
from cryptography.exceptions import InvalidTag
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

HEADER = "TENCDEC:1:"
AAD_TAG = b"TENCDEC:1"
SALT, NONCE, TAG, BLOCK = 16, 12, 16, 64
DEVICE_TAG = b"PC-TEST\x00"  # feste Kennung für dieses Test-Script


def normalize(phrase: str) -> bytes:
    text = unicodedata.normalize("NFC", phrase).lower().strip()
    return re.sub(r"\s+", " ", text).encode("utf-8")


def derive(phrase: bytes, salt: bytes) -> bytes:
    return hash_secret_raw(phrase, salt, time_cost=3, memory_cost=64 * 1024,
                           parallelism=1, hash_len=32, type=Type.ID, version=19)


def b64e(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode().rstrip("=")


def b64d(text: str) -> bytes:
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


def encrypt(text: str, phrase: bytes, name: str) -> str:
    name_b = name.encode("utf-8")[:32]
    payload = struct.pack(">q", int(datetime.datetime.now().timestamp())) + DEVICE_TAG
    payload += bytes([len(name_b)]) + name_b + text.encode("utf-8")
    padded = payload + b"\x80" + b"\x00" * (BLOCK - 1 - len(payload) % BLOCK)
    salt, nonce = os.urandom(SALT), os.urandom(NONCE)
    sealed = AESGCM(derive(phrase, salt)).encrypt(nonce, padded, AAD_TAG + salt)
    return HEADER + b64e(salt + nonce + sealed)


def decrypt(message: str, phrase: bytes) -> str:
    match = re.search(r"TENCDEC:(\d+):([A-Za-z0-9_-]+)", message)
    if not match:
        return "Keine TENCDEC-Nachricht gefunden."
    if match.group(1) != "1":
        return f"Version {match.group(1)} wird nicht unterstützt."
    raw = b64d(match.group(2))
    salt, nonce, sealed = raw[:SALT], raw[SALT:SALT + NONCE], raw[SALT + NONCE:]
    try:
        padded = AESGCM(derive(phrase, salt)).decrypt(nonce, sealed, AAD_TAG + salt)
    except InvalidTag:
        return "Falscher Satz oder veränderte Nachricht."
    payload = padded.rstrip(b"\x00")[:-1]
    sent_at = struct.unpack(">q", payload[:8])[0]
    name_len = payload[16]
    name = payload[17:17 + name_len].decode("utf-8")
    text = payload[17 + name_len:].decode("utf-8")
    when = datetime.datetime.fromtimestamp(sent_at).strftime("%d.%m.%Y %H:%M")
    return f"Von: {name} · {when}\n{text}"


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")  # Windows-Konsole: Umlaute und Emojis
    phrase = normalize(getpass.getpass("Sicherheitssatz (unsichtbar): "))
    name = input("Dein Name [PC]: ").strip() or "PC"
    while True:
        line = input("\nNachricht oder TENCDEC-Text einfügen (leer = Ende): ").strip()
        if not line:
            break
        print(decrypt(line, phrase) if "TENCDEC:" in line else encrypt(line, phrase, name))


if __name__ == "__main__":
    try:
        main()
    except (KeyboardInterrupt, EOFError):
        sys.exit()
