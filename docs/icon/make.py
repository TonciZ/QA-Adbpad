from PIL import Image, ImageDraw
S = 4096
TILE = (0x3B, 0x73, 0x96, 255)
WHITE = (250, 252, 253, 255)
ACCENT = (255, 190, 60, 255)

def build():
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, S - 1, S - 1), radius=int(S * 0.22), fill=TILE)
    x0, x1 = int(S * 0.19), int(S * 0.81)
    y0, y1 = int(S * 0.26), int(S * 0.68)
    d.rounded_rectangle((x0, y0, x1, y1), radius=int(S * 0.05), fill=WHITE)
    t = int(S * 0.05)
    d.rounded_rectangle((x0 + t, y0 + t, x1 - t, y1 - t), radius=int(S * 0.025), fill=TILE)
    # TV feet: two short legs, wide apart (reads as TV, not monitor)
    fw = int(S * 0.07)
    for fx in (int(S * 0.29), int(S * 0.64)):
        d.rounded_rectangle((fx, y1 - 20, fx + fw, int(S * 0.76)), radius=int(S * 0.02), fill=WHITE)
    cx = S // 2
    w = int(S * 0.09)
    cy = (y0 + y1) // 2
    p1 = (cx - int(S * 0.15), cy + int(S * 0.0))
    p2 = (cx - int(S * 0.04), cy + int(S * 0.11))
    p3 = (cx + int(S * 0.16), cy - int(S * 0.10))
    d.line([p1, p2, p3], fill=ACCENT, width=w, joint="curve")
    for p in (p1, p3):
        r = w // 2
        d.ellipse((p[0] - r, p[1] - r, p[0] + r, p[1] + r), fill=ACCENT)
    return im.resize((1024, 1024), Image.LANCZOS)

def preview(icon):
    sizes = [256, 64, 32, 16]
    W = sum(s + 40 for s in sizes) + 40
    sheet = Image.new("RGBA", (W, 640), (255, 255, 255, 255))
    ImageDraw.Draw(sheet).rectangle((0, 320, W, 640), fill=(0x20, 0x20, 0x20, 255))
    for row, y in enumerate((30, 350)):
        x = 40
        for s in sizes:
            ic = icon.resize((s, s), Image.LANCZOS)
            sheet.alpha_composite(ic, (x, y + (256 - s) // 2))
            x += s + 40
    sheet.convert("RGB").save("preview.png")

if __name__ == "__main__":
    ic = build()
    ic.save("icon_1024.png")
    preview(ic)
