import math, random, os
from PIL import Image, ImageDraw, ImageFilter, ImageChops

OUT = '/home/claude/elementalawakening/src/main/resources/assets/elementalawakening/textures/entity/circle/'
os.makedirs(OUT, exist_ok=True)

S = 1024           # desenha em 1024 e reduz (suaviza)
C = S // 2
K = S / 512.0      # coordenadas do desenho: 512 unidades, centro em (0,0), y para baixo

# ---------------------------------------------------------------- helpers
def P(x, y): return (C + x * K, C + y * K)
def pol(r, deg):
    a = math.radians(deg); return (r * math.cos(a), r * math.sin(a))
def new():
    m = Image.new('L', (S, S), 0); return m, ImageDraw.Draw(m)

def seg(d, a, b, w=2.5, val=255):
    pa, pb = P(*a), P(*b)
    d.line([pa, pb], fill=val, width=max(1, int(w * K)))
    rr = w * K / 2
    for p in (pa, pb):
        d.ellipse([p[0]-rr, p[1]-rr, p[0]+rr, p[1]+rr], fill=val)

def poly(d, pts, w=2.5, closed=True, fill=None, val=255):
    if fill is not None and closed:
        d.polygon([P(*p) for p in pts], fill=fill)
    n = len(pts)
    for i in range(n if closed else n - 1):
        seg(d, pts[i], pts[(i + 1) % n], w, val)

def circ(d, cx, cy, r, w=2.5, fill=None):
    x0, y0 = P(cx - r, cy - r); x1, y1 = P(cx + r, cy + r)
    if fill is not None: d.ellipse([x0, y0, x1, y1], fill=fill)
    if w: d.ellipse([x0, y0, x1, y1], outline=255, width=max(1, int(w * K)))

def arc_pts(cx, cy, r, a0, a1, n=64):
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / n)),
             cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / n))) for i in range(n + 1)]

def curve(f, t0, t1, n=80):
    return [f(t0 + (t1 - t0) * i / n) for i in range(n + 1)]

def spiral(r0, r1, a0, turns_deg, n=80):
    return curve(lambda t: pol(r0 + (r1 - r0) * t, a0 + turns_deg * t), 0, 1, n)

def drop(cx, cy, s, rot=-90, tip=2.3):
    pts = []
    for i in range(61):
        a = rot + 35 + 290 * i / 60
        pts.append((cx + s * math.cos(math.radians(a)), cy + s * math.sin(math.radians(a))))
    ang = math.radians(rot)
    pts.append((cx + tip * s * math.cos(ang), cy + tip * s * math.sin(ang)))
    return pts

def catmull(pts, n=10):
    out = []; m = len(pts)
    for i in range(m):
        p0, p1, p2, p3 = pts[(i-1) % m], pts[i], pts[(i+1) % m], pts[(i+2) % m]
        for j in range(n):
            t = j / n; t2 = t * t; t3 = t2 * t
            out.append(tuple(0.5 * ((2*p1[k]) + (-p0[k]+p2[k])*t + (2*p0[k]-5*p1[k]+4*p2[k]-p3[k])*t2
                                    + (-p0[k]+3*p1[k]-3*p2[k]+p3[k])*t3) for k in (0, 1)))
    return out

def rot_pts(pts, deg):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    return [(x*c - y*s, x*s + y*c) for x, y in pts]

def ellipse_pts(rx, ry, rot, n=80):
    return rot_pts([(rx*math.cos(2*math.pi*i/n), ry*math.sin(2*math.pi*i/n)) for i in range(n)], rot)

def rune(d, rc, deg, size, rng):
    pts = [(x, y) for x in (-1, 0, 1) for y in (-1, 0, 1)]
    chosen = rng.sample(pts, rng.choice([3, 4, 4, 5]))
    chosen.sort(key=lambda p: (p[1], p[0]))
    segs = [((0, -1), (0, 1))] + [(chosen[i], chosen[i+1]) for i in range(len(chosen)-1)]
    ang = math.radians(deg); ca, sa = math.cos(ang), math.sin(ang)
    cx, cy = rc * ca, rc * sa
    def tr(p):
        tx, ty = p[0] * size * 0.5, p[1] * size * 0.5
        return (cx + tx * (-sa) + ty * ca, cy + tx * ca + ty * sa)
    for a, b in segs: seg(d, tr(a), tr(b), 2.2)

def finish(mask, name, crop=None):
    glow = mask.filter(ImageFilter.GaussianBlur(3.5 * K)).point(lambda v: min(255, int(v * 1.6)))
    comb = ImageChops.lighter(mask, glow.point(lambda v: int(v * 0.65)))
    if crop:
        comb = comb.crop((int(C - crop*K), int(C - crop*K), int(C + crop*K), int(C + crop*K)))
    comb = comb.resize((512, 512), Image.LANCZOS)
    # fundo PRETO e opaco: o desenho e a propria luz (brilho aditivo no jogo; tambem aparece em qualquer galeria)
    Image.merge('RGB', (comb, comb, comb)).convert('RGBA').save(OUT + name)

# ---------------------------------------------------------------- camada EXTERNA (aneis + runas)
def make_outer(name, seed):
    rng = random.Random(seed)
    m, d = new()
    for r, w in ((250, 5), (241, 2), (228, 3), (186, 3), (180, 1.5)): circ(d, 0, 0, r, w)
    for i in range(72):
        seg(d, pol(232, i*5), pol(238, i*5), 3 if i % 6 == 0 else 1.6)
    for i in range(24):
        rune(d, 207, i*15 + 7.5, 24, rng)
        seg(d, pol(188, i*15), pol(226, i*15), 1.5)
    for i in range(8):
        x, y = pol(250, i*45); rr = 6
        poly(d, [(x, y-rr), (x+rr, y), (x, y+rr), (x-rr, y)], 1, True, fill=255)
    finish(m, f'outer_{name}.png')

# ---------------------------------------------------------------- camada FRAME (entre 98 e 172)
def frame_base(d):
    for r, w in ((172, 4), (164, 1.5), (98, 3), (90, 1.5)): circ(d, 0, 0, r, w)

def dot(d, p, r=5): circ(d, p[0], p[1], r, 0, 255)

def f_fire(d):
    poly(d, [pol(168 if i % 2 == 0 else 122, -90 + i*22.5) for i in range(16)], 3.5)
    for i in range(8): dot(d, pol(112, -90 + i*45 + 22.5), 5)

def f_water(d):
    for base, amp, ph in ((138, 9, 0), (154, 7, math.pi)):
        poly(d, [pol(base + amp*math.sin(12*t + ph), math.degrees(t)) for t in [2*math.pi*i/240 for i in range(240)]], 2.8)
    for i in range(12): dot(d, pol(118, i*30 + 15), 4)

def f_lightning(d):
    poly(d, [pol(166 if i % 2 == 0 else 116, i*15) for i in range(24)], 3.2)
    for i in range(6):
        a = i*60 + 30
        seg(d, pol(104, a), pol(120, a+6), 2.5); seg(d, pol(120, a+6), pol(112, a+12), 2.5)

def f_earth(d):
    for rot in (0, 45): poly(d, [pol(168, rot + 90*i) for i in range(4)], 3.5)
    poly(d, [pol(122, 22.5 + 45*i) for i in range(8)], 2.5)
    for i in range(8): dot(d, pol(150, 22.5 + 45*i), 4)

def f_wind(d):
    for k in range(6):
        pts = spiral(100, 166, k*60, 110, 50); poly(d, pts, 3, False); dot(d, pts[-1], 5)

def f_light(d):
    for i in range(12):
        a = i*30; poly(d, [pol(112, a-5), pol(168, a), pol(112, a+5)], 2.8)
    circ(d, 0, 0, 126, 2)
    for i in range(12): dot(d, pol(126, i*30 + 15), 4)

def f_darkness(d):
    vs = [pol(168, 90 + 72*i) for i in range(5)]
    for i in range(5): seg(d, vs[i], vs[(i+2) % 5], 3.5)
    circ(d, 0, 0, 128, 2)
    for v in vs: circ(d, v[0], v[1], 8, 2.5)

def f_metal(d):
    pts = []
    for i in range(16):
        a = i*22.5
        for da, r in ((-9, 140), (-6, 158), (6, 158), (9, 140)): pts.append(pol(r, a+da))
    poly(d, pts, 3)
    circ(d, 0, 0, 124, 2.5)
    for i in range(6):
        x, y = pol(111, i*60 + 30); poly(d, [(x + 7*math.cos(math.radians(60*j)), y + 7*math.sin(math.radians(60*j))) for j in range(6)], 2)

def f_eclipse(d):
    for i in range(36):
        a = i*10; seg(d, pol(112, a), pol(160 if i % 2 == 0 else 142, a), 2.8 if i % 2 == 0 else 2)
    circ(d, 0, 0, 135, 2.5); circ(d, 22, 0, 135, 2)

def f_blood(d):
    for i in range(6):
        a = -90 + 60*i; c = pol(132, a); poly(d, drop(c[0], c[1], 12, a, 2.4), 3, True, fill=70)
    circ(d, 0, 0, 112, 2)
    for i in range(6): dot(d, pol(150, -60 + 60*i), 4)

def f_celestial(d):
    vs = [pol(168, -90 + 72*i) for i in range(5)]
    for i in range(5): seg(d, vs[i], vs[(i+2) % 5], 3)
    circ(d, 0, 0, 122, 2)
    for i in range(5): circ(d, vs[i][0], vs[i][1], 7, 2.5, 255)
    for i in range(10): dot(d, pol(122, -90 + 36*i), 3.5)

def f_gravity(d):
    for base in (0, 120, 240):
        pts = spiral(100, 168, base, 480, 90); poly(d, pts, 3, False)
        for i in (30, 60): dot(d, pts[i], 4)

def f_time(d):
    for i in range(60):
        a = i*6 - 90
        if i % 5 == 0: seg(d, pol(136, a), pol(164, a), 3.5)
        else: seg(d, pol(152, a), pol(164, a), 1.8)
    for i in range(12): dot(d, pol(120, i*30 - 90), 4)

def f_reality(d):
    rng = random.Random(99)
    for _ in range(14):
        a0 = rng.uniform(0, 360); circ_r = 135 + rng.uniform(-14, 14)
        poly(d, arc_pts(0, 0, circ_r, a0, a0 + rng.uniform(10, 28), 12), 3, False)
    for _ in range(10):
        a = rng.uniform(0, 360); r = 100; pts = [pol(r, a)]
        for _ in range(4):
            r += rng.uniform(12, 20); a += rng.uniform(-9, 9); pts.append(pol(min(r, 168), a))
        poly(d, pts, 2.2, False)
    for _ in range(6):
        a = rng.uniform(0, 360); c = pol(rng.uniform(112, 160), a)
        poly(d, [(c[0] + 9*math.cos(math.radians(a + 120*j)), c[1] + 9*math.sin(math.radians(a + 120*j))) for j in range(3)], 2, True, fill=90)

FRAMES = dict(fire=f_fire, water=f_water, lightning=f_lightning, earth=f_earth, wind=f_wind, light=f_light,
              darkness=f_darkness, metal=f_metal, eclipse=f_eclipse, blood=f_blood, celestial=f_celestial,
              gravity=f_gravity, time=f_time, reality_collapse=f_reality)

# ---------------------------------------------------------------- EMBLEMA central (raio < 90)
def e_fire(d):
    outer = [(0,-88),(0,-88),(16,-60),(34,-38),(46,-6),(42,28),(26,58),(0,78),(-26,58),(-42,28),(-44,-4),(-34,-30),(-20,-18),(-14,-48)]
    poly(d, catmull(outer, 10), 3.5, True, fill=60)
    inner = [(0,-30),(0,-30),(14,-8),(22,22),(12,48),(0,56),(-12,48),(-22,22),(-12,0),(-6,-14)]
    poly(d, catmull(inner, 10), 3, True, fill=120)

def e_water(d):
    poly(d, drop(0, 22, 52, -90, 2.2), 3.5, True, fill=60)
    poly(d, arc_pts(0, 22, 32, 110, 175, 14), 3, False)
    poly(d, drop(-70, 46, 9, -90, 2.2), 2.5, True, fill=90)
    poly(d, drop(68, 58, 7, -90, 2.2), 2.5, True, fill=90)

def e_lightning(d):
    poly(d, [(25,-88),(-35,5),(-2,5),(-22,88),(40,-22),(4,-22)], 3.5, True, fill=70)
    for a in (-160, -20, 20, 160): seg(d, pol(62, a), pol(80, a), 2.5)

def e_earth(d):
    poly(d, [(-85,66),(-18,-62),(14,-20),(34,-50),(85,66)], 3.5, True, fill=60)
    seg(d, (-18,-62), (-4,10), 2.5); seg(d, (34,-50), (20,20), 2.5)
    seg(d, (-95,66), (95,66), 3)
    poly(d, [(-60,66),(-45,40),(-30,66)], 2.5, True, fill=90)

def e_wind(d):
    for k in range(3):
        pts = spiral(10, 82, k*120, 270, 70); poly(d, pts, 3.5, False); dot(d, pts[-1], 5)

def e_light(d):
    circ(d, 0, 0, 30, 3.5, 90); circ(d, 0, 0, 42, 2)
    for i in range(16):
        a = i*22.5; seg(d, pol(52, a), pol(88 if i % 2 == 0 else 68, a), 3 if i % 2 == 0 else 2.5)

def e_darkness(d):
    pts = arc_pts(0, 0, 70, 54.5, 305.5, 60) + arc_pts(30, 0, 58, 280.5, 79.5, 60)
    poly(d, pts, 3.5, True, fill=60)
    for p in ((58, -34), (66, 0), (58, 34)): dot(d, p, 4)

def e_metal(d):
    pts = []
    for i in range(8):
        a = i*45
        for da, r in ((-14, 58), (-9, 78), (9, 78), (14, 58)): pts.append(pol(r, a+da))
    poly(d, pts, 3.5, True, fill=60)
    circ(d, 0, 0, 26, 3.5); circ(d, 0, 0, 50, 2)
    for i in range(6): seg(d, pol(26, i*60), pol(50, i*60), 2.5)

def e_eclipse(d):
    circ(d, 0, 0, 46, 3.5, 100); circ(d, 14, 0, 46, 2)
    for i in range(24):
        a = i*15; seg(d, pol(58, a), pol(88 if i % 2 == 0 else 70, a), 2.5)

def e_blood(d):
    poly(d, drop(0, 22, 48, -90, 2.3), 3.5, True, fill=70)
    seg(d, (0,-10), (0,56), 3); seg(d, (-26,22), (26,22), 3)
    poly(d, drop(-68, -4, 8, -90, 2.2), 2.5, True, fill=110)
    poly(d, drop(68, -4, 8, -90, 2.2), 2.5, True, fill=110)
    poly(d, drop(-52, 62, 6, -90, 2.2), 2.2, True, fill=110)
    poly(d, drop(52, 62, 6, -90, 2.2), 2.2, True, fill=110)

def e_celestial(d):
    poly(d, [pol(88 if i % 2 == 0 else 36, -90 + i*22.5) for i in range(16)], 3.5, True, fill=60)
    circ(d, 0, 0, 16, 3, 255)
    for i in range(4): dot(d, pol(66, 45 + 90*i), 5)

def e_gravity(d):
    for rot in (-28, 28, 90): poly(d, ellipse_pts(82, 26, rot), 2.8)
    circ(d, 0, 0, 18, 3, 255)
    for rot, t in ((-28, 40), (28, 5), (90, 60)):
        pts = ellipse_pts(82, 26, rot); dot(d, pts[t], 5)

def e_time(d):
    seg(d, (-52,-86), (52,-86), 4); seg(d, (-52,86), (52,86), 4)
    poly(d, [(-46,-86), (-6,0), (-46,86)], 3.5, False)
    poly(d, [(46,-86), (6,0), (46,86)], 3.5, False)
    poly(d, [(-34,80), (34,80), (0,40)], 2.5, True, fill=100)
    poly(d, [(-34,-80), (34,-80), (0,-52)], 2.5, True, fill=60)
    seg(d, (0,-4), (0,38), 2)

def e_reality(d):
    rng = random.Random(5)
    T, R, B, L, O = (0,-82), (82,0), (0,82), (-82,0), (4,-3)
    for tri in ((T,R,O), (R,B,O), (B,L,O), (L,T,O)):
        cx = sum(p[0] for p in tri)/3; cy = sum(p[1] for p in tri)/3
        n = math.hypot(cx, cy) or 1; ox, oy = cx/n*7, cy/n*7
        poly(d, [(p[0]+ox, p[1]+oy) for p in tri], 3, True, fill=55)
    poly(d, [pol(18, 45 + 90*i) for i in range(4)], 2.5)
    for _ in range(5):
        a = rng.uniform(0, 360); c = pol(rng.uniform(88, 96), a)
        poly(d, [(c[0] + 5*math.cos(math.radians(a + 120*j)), c[1] + 5*math.sin(math.radians(a + 120*j))) for j in range(3)], 2, True, fill=120)

EMBLEMS = dict(fire=e_fire, water=e_water, lightning=e_lightning, earth=e_earth, wind=e_wind, light=e_light,
               darkness=e_darkness, metal=e_metal, eclipse=e_eclipse, blood=e_blood, celestial=e_celestial,
               gravity=e_gravity, time=e_time, reality_collapse=e_reality)

ORDER = ['fire','water','lightning','earth','wind','light','darkness','metal','eclipse','blood','celestial','gravity','time','reality_collapse']
for idx, name in enumerate(ORDER):
    make_outer(name, 100 + idx)
    m, d = new(); frame_base(d); FRAMES[name](d); finish(m, f'frame_{name}.png')
    m, d = new(); EMBLEMS[name](d); finish(m, f'emblem_{name}.png', crop=100)

# ---------------------------------------------------------------- TEMPO: relogio com numeros romanos (como no jogo)
from PIL import ImageFont
ROMAN = ['XII','I','II','III','IV','V','VI','VII','VIII','IX','X','XI']

def numeral(mask, text, r, deg, size):
    font = ImageFont.truetype('DejaVuSerif-Bold.ttf', int(size * K))
    tmp = Image.new('L', (int(140 * K), int(80 * K)), 0)
    td = ImageDraw.Draw(tmp)
    bb = td.textbbox((0, 0), text, font=font)
    w, h = bb[2] - bb[0], bb[3] - bb[1]
    td.text(((tmp.width - w) / 2 - bb[0], (tmp.height - h) / 2 - bb[1]), text, fill=255, font=font)
    rot = tmp.rotate(-(deg + 90), expand=True, resample=Image.BICUBIC)
    cx, cy = C + math.cos(math.radians(deg)) * r * K, C + math.sin(math.radians(deg)) * r * K
    x, y = int(cx - rot.width / 2), int(cy - rot.height / 2)
    region = mask.crop((x, y, x + rot.width, y + rot.height))
    mask.paste(ImageChops.lighter(region, rot), (x, y))

# camada externa: aneis + 12 numerais grandes + 60 marcas
m, d = new()
for r, w in ((250, 5), (241, 2), (192, 3), (186, 1.5)): circ(d, 0, 0, r, w)
for i in range(60):
    seg(d, pol(244, i*6), pol(249 if i % 5 else 247, i*6), 2.5 if i % 5 == 0 else 1.2)
for i in range(12):
    numeral(m, ROMAN[i], 214, -90 + 30*i, 40)
finish(m, 'outer_time.png')

# camada interna: relogio menor com numerais + espiral no centro
m, d = new()
for r, w in ((172, 4), (164, 1.5), (98, 3), (90, 1.5)): circ(d, 0, 0, r, w)
for i in range(60):
    seg(d, pol(160, i*6), pol(164 if i % 5 else 168, i*6), 2 if i % 5 == 0 else 1)
for i in range(12):
    numeral(m, ROMAN[i], 130, -90 + 30*i, 26)
poly(d, spiral(8, 82, -90, 1080, 160), 2.5, False)
poly(d, spiral(8, 82, 90, 1080, 160), 2.5, False)
finish(m, 'frame_time.png')

print('ok', len(os.listdir(OUT)), 'arquivos')
