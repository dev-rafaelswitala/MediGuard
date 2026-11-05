from PIL import Image

src = Image.open(r"branding/Logo-Icons/transparent-logo.png").convert("RGBA")
pixels = src.load()
w, h = src.size
minx, miny, maxx, maxy = w, h, 0, 0
for y in range(h):
    for x in range(w):
        r, g, b, a = pixels[x, y]
        if a > 20 and not (r < 30 and g < 30 and b < 30):
            minx = min(minx, x)
            miny = min(miny, y)
            maxx = max(maxx, x)
            maxy = max(maxy, y)

pad = 16
minx = max(0, minx - pad)
miny = max(0, miny - pad)
maxx = min(w - 1, maxx + pad)
maxy = min(h - 1, maxy + pad)
cropped = src.crop((minx, miny, maxx + 1, maxy + 1))
cp = cropped.load()
cw, ch = cropped.size
for y in range(ch):
    for x in range(cw):
        r, g, b, a = cp[x, y]
        if a < 20 or (r < 30 and g < 30 and b < 30):
            cp[x, y] = (0, 0, 0, 0)

canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
max_w = int(1024 * 0.58)
scale = min(max_w / cw, max_w / ch)
nw, nh = int(cw * scale), int(ch * scale)
resized = cropped.resize((nw, nh), Image.Resampling.LANCZOS)
ox, oy = (1024 - nw) // 2, (1024 - nh) // 2
canvas.paste(resized, (ox, oy), resized)
out = r"app/src/main/res/drawable-nodpi/ic_splash_brand.png"
canvas.save(out)
print(f"saved {out} logo {nw}x{nh} at ({ox},{oy})")
