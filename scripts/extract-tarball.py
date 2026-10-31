#!/usr/bin/env python3
"""Extract a tar.gz, materializing symlinks/hardlinks as regular-file copies.

Windows hosts cannot create POSIX symlinks without developer mode/admin, and the
pinned Temurin linux/mac tarballs contain symlinked license files. This helper
performs a controlled extraction used by scripts/stage-eyecode-jdk.ps1.
"""
import os
import shutil
import sys
import tarfile


def resolve_member(members_by_name, name, linkname, is_symlink):
    if is_symlink:
        base = os.path.dirname(name)
        current = os.path.normpath(os.path.join(base, linkname)).replace("\\", "/")
    else:
        current = linkname.lstrip("/")
    seen = set()
    while current not in seen:
        seen.add(current)
        member = members_by_name.get(current)
        if member is None:
            return None
        if member.isfile():
            return member
        if member.issym():
            current = os.path.normpath(
                os.path.join(os.path.dirname(member.name), member.linkname)
            ).replace("\\", "/")
            continue
        if member.islnk():
            current = member.linkname.lstrip("/")
            continue
        return None
    return None


def main():
    if len(sys.argv) != 3:
        print("usage: extract-tarball.py <archive.tar.gz> <dest-dir>", file=sys.stderr)
        return 2
    archive, dest = sys.argv[1], sys.argv[2]
    os.makedirs(dest, exist_ok=True)
    materialized = 0
    warnings = 0
    with tarfile.open(archive, "r:gz") as tf:
        members = tf.getmembers()
        members_by_name = {m.name: m for m in members}
        links = []
        for member in members:
            out_path = os.path.join(dest, member.name)
            if member.isdir():
                os.makedirs(out_path, exist_ok=True)
            elif member.isfile():
                os.makedirs(os.path.dirname(out_path), exist_ok=True)
                src = tf.extractfile(member)
                with open(out_path, "wb") as out:
                    shutil.copyfileobj(src, out)
                os.chmod(out_path, 0o755 if member.mode & 0o111 else 0o644)
            elif member.issym() or member.islnk():
                links.append(member)
        for member in links:
            out_path = os.path.join(dest, member.name)
            os.makedirs(os.path.dirname(out_path), exist_ok=True)
            source = resolve_member(
                members_by_name, member.name, member.linkname, member.issym()
            )
            if source is None:
                print(
                    f"warning: unresolved link {member.name} -> {member.linkname}",
                    file=sys.stderr,
                )
                with open(out_path, "wb"):
                    pass
                warnings += 1
                continue
            src = tf.extractfile(source)
            with open(out_path, "wb") as out:
                shutil.copyfileobj(src, out)
            mode = source.mode if source.mode else member.mode
            os.chmod(out_path, 0o755 if mode & 0o111 else 0o644)
            materialized += 1
    print(
        f"extracted {len(members)} members "
        f"({materialized} links materialized, {warnings} warnings) to {dest}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
