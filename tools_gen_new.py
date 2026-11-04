# Gera as texturas (outer / frame / emblem) dos 30 elementos novos. Usa as funcoes de gen_circles.py.
src = open('/home/claude/gen_circles.py', encoding='utf-8').read()
exec(src[:src.index('def f_fire')])      # helpers + make_outer + frame_base + dot

# ------------------------------------------------------------------ figuras do FRAME (entre r=100 e r=170)
def fr_star(d, n, step, r=168, w=3.2, rot=-90):
    vs = [pol(r, rot + 360.0 * i / n) for i in range(n)]
    for i in range(n): seg(d, vs[i], vs[(i + step) % n], w)

def fr_polys(d, sides, r=168, w=3.0):
    for k, n in enumerate(sides):
        poly(d, [pol(r - 10 * k, -90 + 360.0 * i / n + (180.0 / n) * k) for i in range(n)], w)

def fr_petals(d, n, r0=104, r1=166, w=2.8):
    for i in range(n):
        a = 360.0 * i / n
        pts = [pol(r0, a)] + [pol((r0 + r1) / 2 + 6, a - 360.0 / n * 0.42), pol(r1, a), pol((r0 + r1) / 2 + 6, a + 360.0 / n * 0.42)]
        poly(d, pts, w)

def fr_rays(d, n, r0=110, r1=168, w=3.0):
    for i in range(n):
        a = 360.0 * i / n
        seg(d, pol(r0, a), pol(r1 if i % 2 == 0 else r1 - 20, a), w if i % 2 == 0 else w - 1)

def fr_gear(d, n, r0=140, r1=160):
    pts = []
    for i in range(n):
        a = 360.0 * i / n; s = 180.0 / n
        for da, r in ((-s * 0.6, r0), (-s * 0.35, r1), (s * 0.35, r1), (s * 0.6, r0)): pts.append(pol(r, a + da))
    poly(d, pts, 3)

def fr_waves(d, base, amp, freq, w=2.6):
    poly(d, [pol(base + amp * math.sin(freq * t), math.degrees(t)) for t in [2 * math.pi * i / 240 for i in range(240)]], w)

def fr_spiral(d, k, turns=110):
    for j in range(k):
        pts = spiral(100, 166, j * 360.0 / k, turns, 50); poly(d, pts, 3, False)

def fr_orbits(d, k, rx=160, ry=60):
    for j in range(k): poly(d, ellipse_pts(rx, ry, j * 180.0 / k), 2.6)

def fr_dots(d, n, r=135, rr=5):
    for i in range(n): circ(d, *pol(r, 360.0 * i / n), rr, 0, 255)

def fr_cracks(d, seed, n=9):
    rng = random.Random(seed)
    for _ in range(n):
        a = rng.uniform(0, 360); r = 100; pts = [pol(r, a)]
        for _ in range(4):
            r += rng.uniform(12, 20); a += rng.uniform(-10, 10); pts.append(pol(min(r, 168), a))
        poly(d, pts, 2.4, False)

def fr_lattice(d, n=6):
    for i in range(n):
        a = 360.0 * i / n
        seg(d, pol(100, a), pol(168, a + 360.0 / n / 2), 2.4); seg(d, pol(168, a), pol(100, a + 360.0 / n / 2), 2.4)
    circ(d, 0, 0, 134, 2)

FRAMES2 = {
  'ice':      lambda d: (fr_star(d, 12, 5), fr_rays(d, 12, 108, 130, 2), fr_dots(d, 12, 150, 4)),
  'nature':   lambda d: (fr_petals(d, 8), fr_spiral(d, 4), fr_dots(d, 8, 120, 4)),
  'poison':   lambda d: (fr_waves(d, 140, 10, 9), fr_waves(d, 152, 7, 9), fr_dots(d, 9, 118, 5)),
  'sand':     lambda d: (fr_polys(d, [3, 3], 168), fr_rays(d, 24, 108, 150, 2), fr_dots(d, 6, 128, 5)),
  'steam':    lambda d: (fr_waves(d, 135, 12, 6), fr_waves(d, 155, 9, 6), fr_orbits(d, 3, 150, 40)),
  'magma':    lambda d: (fr_rays(d, 14, 110, 168), fr_polys(d, [7], 130), fr_cracks(d, 3, 6)),
  'storm':    lambda d: (fr_spiral(d, 3, 180), fr_orbits(d, 3, 160, 55), fr_dots(d, 12, 120, 4)),
  'crystal':  lambda d: (fr_polys(d, [6, 6], 168), fr_lattice(d, 6)),
  'sound':    lambda d: (circ(d, 0, 0, 120, 2.4), circ(d, 0, 0, 138, 2.4), circ(d, 0, 0, 156, 2.4), fr_rays(d, 16, 110, 160, 2)),
  'shadow':   lambda d: (fr_spiral(d, 5, -120), fr_dots(d, 10, 150, 4), fr_polys(d, [5], 128)),
  'acid':     lambda d: (fr_waves(d, 145, 8, 14), fr_dots(d, 14, 118, 5), fr_polys(d, [6], 125)),
  'glass':    lambda d: (fr_cracks(d, 11, 11), fr_polys(d, [5, 5], 168)),
  'sakura':   lambda d: (fr_petals(d, 5, 104, 168), fr_petals(d, 5, 104, 140), fr_dots(d, 5, 150, 6)),
  'bone':     lambda d: (fr_polys(d, [8], 168), fr_polys(d, [8], 128), fr_dots(d, 8, 148, 5)),
  'spirit':   lambda d: (fr_orbits(d, 4, 165, 70), fr_dots(d, 4, 118, 6)),
  'plasma':   lambda d: (fr_orbits(d, 3, 168, 90), fr_star(d, 7, 3, 150), fr_dots(d, 6, 118, 5)),
  'dream':    lambda d: (fr_orbits(d, 5, 160, 40), fr_dots(d, 20, 140, 3), fr_star(d, 5, 2, 130)),
  'nightmare':lambda d: (fr_rays(d, 20, 108, 168), fr_polys(d, [3], 168), fr_cracks(d, 5, 7)),
  'mirror':   lambda d: (fr_polys(d, [4, 4], 168), fr_lattice(d, 8)),
  'rune':     lambda d: (fr_polys(d, [8, 8], 168), fr_dots(d, 16, 128, 4), fr_gear(d, 8, 120, 140)),
  'comet':    lambda d: (fr_spiral(d, 2, 140), fr_rays(d, 8, 110, 168), fr_dots(d, 16, 150, 3)),
  'cosmos':   lambda d: (fr_spiral(d, 2, 300), fr_orbits(d, 3, 165, 45), fr_dots(d, 24, 140, 3)),
  'chaos':    lambda d: (fr_star(d, 8, 3, 168), fr_star(d, 5, 2, 130), fr_cracks(d, 8, 6)),
  'order':    lambda d: (fr_polys(d, [3, 4, 6, 8], 168, 2.6), fr_dots(d, 12, 140, 4)),
  'life':     lambda d: (fr_petals(d, 12, 104, 166), fr_spiral(d, 6, 80)),
  'death':    lambda d: (fr_polys(d, [3, 3], 168), fr_cracks(d, 13, 8), fr_dots(d, 6, 118, 6)),
  'dragon':   lambda d: (fr_gear(d, 12, 140, 162), fr_star(d, 6, 2, 130), fr_rays(d, 12, 108, 130, 2)),
  'void':     lambda d: (fr_spiral(d, 4, -200), fr_polys(d, [5], 150), fr_dots(d, 5, 118, 7)),
  'creation': lambda d: (fr_petals(d, 12, 104, 168), fr_polys(d, [12], 125), fr_dots(d, 12, 150, 4)),
  'eternity': lambda d: (fr_orbits(d, 6, 165, 70), fr_dots(d, 24, 140, 3), fr_polys(d, [6], 128)),
}

# ------------------------------------------------------------------ EMBLEMAS (raio < 90)
def lens(d, cx, cy, length, width, rot, w=2.8, fill=70):
    pts = []
    for i in range(0, 25):
        t = i / 24.0
        pts.append((length * (t - 0.5), width * math.sin(math.pi * t)))
    pts += [(length * (0.5 - i / 24.0), -width * math.sin(math.pi * (1 - i / 24.0))) for i in range(0, 25)]
    c, s = math.cos(math.radians(rot)), math.sin(math.radians(rot))
    poly(d, [(cx + x * c - y * s, cy + x * s + y * c) for x, y in pts], w, True, fill=fill)

def cloud(d, cx, cy, sc=1.0, fill=70):
    for x, y, r in ((-30, 8, 26), (4, -10, 34), (40, 10, 24)):
        circ(d, cx + x * sc, cy + y * sc, r * sc, 3, fill)
    seg(d, (cx - 56 * sc, cy + 30 * sc), (cx + 62 * sc, cy + 30 * sc), 3)

def star4(d, cx, cy, r, w=2.4):
    poly(d, [(cx, cy - r), (cx + r * 0.25, cy - r * 0.25), (cx + r, cy), (cx + r * 0.25, cy + r * 0.25),
             (cx, cy + r), (cx - r * 0.25, cy + r * 0.25), (cx - r, cy), (cx - r * 0.25, cy - r * 0.25)], w, True, fill=120)

def e_ice(d):
    for i in range(6):
        a = i * 60 - 90
        seg(d, (0, 0), pol(86, a), 3.4)
        for t, ln in ((44, 26), (64, 18)):
            b = pol(t, a)
            seg(d, b, (b[0] + pol(ln, a - 42)[0], b[1] + pol(ln, a - 42)[1]), 2.6)
            seg(d, b, (b[0] + pol(ln, a + 42)[0], b[1] + pol(ln, a + 42)[1]), 2.6)
    circ(d, 0, 0, 10, 2.5, 255)

def e_nature(d):
    pts = [(0, 85), (4, 50), (-2, 10), (2, -25)]
    poly(d, catmull(pts + [(0, -40)], 8)[:40], 3.5, False)
    lens(d, -30, 0, 70, 22, -35)
    lens(d, 30, -28, 70, 22, 215)
    lens(d, 0, -62, 54, 16, -90)

def e_poison(d):
    pts = [(-14, -82), (14, -82), (14, -46), (50, 8), (50, 58), (30, 82), (-30, 82), (-50, 58), (-50, 8), (-14, -46)]
    poly(d, pts, 3.5, True, fill=50)
    seg(d, (-48, 14), (48, 14), 2.5)
    circ(d, -14, 38, 9, 2.5); circ(d, 14, 38, 9, 2.5); seg(d, (-8, 60), (8, 60), 2.5)
    for p in ((-30, -10), (-8, -28), (18, -2)): circ(d, p[0], p[1] + 20, 5, 2.2)

def e_sand(d):
    poly(d, [(-82, 62), (0, -56), (82, 62)], 3.5, True, fill=50)
    seg(d, (-41, 3), (41, 3), 2.4); seg(d, (-20, 32), (20, 32), 2.4)
    circ(d, 58, -58, 14, 2.8, 120)
    for k in range(3): poly(d, arc_pts(-30 + k * 30, 76, 18, 200, 340, 12), 2.2, False)

def e_steam(d):
    cloud(d, 0, -22, 0.9)
    for x in (-30, 0, 30):
        poly(d, [(x, 24), (x + 8, 44), (x - 8, 62), (x, 82)], 2.6, False)

def e_magma(d):
    poly(d, [(-86, 72), (-32, -18), (-14, -38), (14, -38), (32, -18), (86, 72)], 3.5, True, fill=50)
    poly(d, catmull([(0, -88), (0, -88), (14, -66), (10, -48), (-10, -48), (-14, -66)], 8), 2.6, True, fill=120)
    for a in (-30, 0, 30): seg(d, (a * 0.4, -30), (a * 1.2, 60), 2.2)
    for p in ((-60, -30), (62, -40), (-72, -62)): circ(d, p[0], p[1], 6, 2.2, 120)

def e_storm(d):
    cloud(d, 0, -40, 0.8)
    poly(d, [(14, -8), (-22, 40), (-2, 40), (-16, 86), (30, 22), (8, 22), (26, -8)], 3.2, True, fill=110)

def e_crystal(d):
    poly(d, [(0, -88), (30, -30), (30, 50), (0, 86), (-30, 50), (-30, -30)], 3.5, True, fill=60)
    for a, b in (((0, -88), (0, 86)), ((-30, -30), (30, 50)), ((30, -30), (-30, 50))): seg(d, a, b, 2)
    poly(d, [(-60, 6), (-48, -22), (-36, 6), (-48, 40)], 2.6, True, fill=60)
    poly(d, [(60, 14), (48, -12), (36, 14), (48, 46)], 2.6, True, fill=60)

def e_sound(d):
    poly(d, [(-76, -22), (-46, -22), (-8, -56), (-8, 56), (-46, 22), (-76, 22)], 3.4, True, fill=60)
    for r in (30, 52, 74): poly(d, arc_pts(-8, 0, r, -48, 48, 18), 3, False)

def e_shadow(d):
    circ(d, 0, 0, 26, 3.4, 120)
    rng = random.Random(4)
    for i in range(9):
        a = i * 40
        pts = [pol(26, a)]
        for k in range(1, 6): pts.append(pol(26 + k * 11, a + 14 * math.sin(k * 1.7 + i)))
        poly(d, pts, 2.8, False)

def e_acid(d):
    pts = [(-22, -86)] + arc_pts(0, 36, 22, 180, 0, 20) + [(22, -86)]
    poly(d, pts, 3.5, True, fill=50)
    seg(d, (-22, -86), (22, -86), 4)
    seg(d, (-22, -4), (22, -4), 2.4)
    for p in ((-8, 30, 6), (8, 50, 5), (-4, 62, 4), (12, 20, 4)): circ(d, p[0], p[1], p[2], 2.2)
    poly(d, drop(-58, 30, 9, -90, 2.2), 2.6, True, fill=90)
    poly(d, drop(56, 52, 7, -90, 2.2), 2.6, True, fill=90)

def e_glass(d):
    rng = random.Random(9)
    circ(d, 0, 0, 70, 3, None)
    for i in range(9):
        a = i * 40 + rng.uniform(-8, 8); pts = [(0, 0)]; r = 0
        for k in range(4):
            r += rng.uniform(14, 22); a += rng.uniform(-9, 9); pts.append(pol(min(r, 86), a))
        poly(d, pts, 2.6, False)
    poly(d, [pol(30, 45 * i) for i in range(8)], 2.2)

def e_sakura(d):
    for i in range(5):
        a = i * 72 - 90
        c = pol(46, a)
        lens(d, c[0], c[1], 54, 22, a, 2.8, 70)
        n = pol(76, a); seg(d, n, (n[0] + pol(8, a + 90)[0], n[1] + pol(8, a + 90)[1]), 2.2)
    for i in range(5): seg(d, (0, 0), pol(26, i * 72 - 90), 2.2); circ(d, *pol(26, i * 72 - 90), 4, 0, 255)
    circ(d, 0, 0, 8, 2.4, 255)

def e_bone(d):
    for sgn in (-1, 1):
        a, b = (-60, -60 * sgn), (60, 60 * sgn)
        seg(d, a, b, 9); seg(d, a, b, 5, 0)
        for e in (a, b):
            for off_ in (-9, 9):
                circ(d, e[0] + off_ * (1 if sgn == 1 else -1) * 0.8, e[1] + off_ * 0.8, 9, 2.6, 80)
    circ(d, 0, -10, 34, 3.4, 40)
    circ(d, -12, -14, 8, 2.4, 160); circ(d, 12, -14, 8, 2.4, 160)
    seg(d, (-12, 18), (12, 18), 2.2); seg(d, (-5, 12), (-5, 22), 2); seg(d, (5, 12), (5, 22), 2)

def e_spirit(d):
    pts = [(-40, 70), (-48, 10), (-34, -48), (0, -74), (34, -48), (48, 10), (40, 70),
           (26, 54), (13, 72), (0, 54), (-13, 72), (-26, 54)]
    poly(d, pts, 3.4, True, fill=55)
    circ(d, -15, -18, 8, 2.6, 200); circ(d, 15, -18, 8, 2.6, 200)
    poly(d, arc_pts(0, 8, 12, 20, 160, 10), 2.4, False)

def e_plasma(d):
    circ(d, 0, 0, 26, 3.4, 140)
    rng = random.Random(2)
    for i in range(8):
        a = i * 45; pts = [pol(26, a)]
        for k in range(1, 5): pts.append(pol(26 + k * 14, a + (10 if k % 2 else -10)))
        poly(d, pts, 2.8, False)

def e_dream(d):
    pts = arc_pts(0, 0, 66, 40, 320, 60) + arc_pts(24, 0, 50, 300, 60, 60)
    poly(d, pts, 3.4, True, fill=55)
    star4(d, 52, -30, 14); star4(d, 70, 14, 9); star4(d, 36, 42, 7)

def e_nightmare(d):
    pts = arc_pts(0, 30, 78, 235, 305, 20) + arc_pts(0, -30, 78, 55, 125, 20)
    poly(d, pts, 3.4, True, fill=50)
    circ(d, 0, 0, 24, 3, 120); circ(d, 0, 0, 10, 0, 255)
    for i in range(7):
        a = -150 + i * 50; b = pol(52, a); c = pol(76, a); seg(d, b, c, 2.4)

def e_mirror(d):
    poly(d, [(0, -88), (52, 0), (0, 88), (-52, 0)], 3.5, True, fill=45)
    poly(d, [(0, -56), (30, 0), (0, 56), (-30, 0)], 2.4)
    seg(d, (-30, -50), (-8, -22), 4); seg(d, (-40, -30), (-20, -8), 2.4)
    seg(d, (0, -88), (0, 88), 1.6)

def e_rune(d):
    seg(d, (0, -86), (0, 86), 4)
    poly(d, [(-46, -52), (0, -18), (46, -52)], 3.6, False)
    poly(d, [(-46, 8), (0, 42), (46, 8)], 3.6, False)
    seg(d, (-30, 74), (30, 74), 3.2); circ(d, 0, -86, 6, 0, 255)

def e_comet(d):
    circ(d, -34, 34, 24, 3.4, 140)
    for i in range(7):
        off_ = -28 + i * 9.5
        seg(d, (-26 + off_ * 0.2, 26 - off_ * 0.2), (62 + off_ * 0.4, -62 + off_ * 0.4), 2.4 if i % 2 else 3.4)
    star4(d, 58, 54, 10)

def e_cosmos(d):
    for base in (0, 180):
        poly(d, spiral(6, 84, base, 300, 90), 3.2, False)
    rng = random.Random(6)
    for _ in range(14):
        p = pol(rng.uniform(20, 84), rng.uniform(0, 360)); circ(d, p[0], p[1], 2.4, 0, 255)
    circ(d, 0, 0, 8, 2.4, 255)

def e_chaos(d):
    for i in range(8):
        a = i * 45
        tip = pol(84, a); seg(d, (0, 0), tip, 3.2)
        poly(d, [tip, pol(66, a - 8), pol(66, a + 8)], 2.4, True, fill=150)
    circ(d, 0, 0, 14, 3, 140)

def e_order(d):
    circ(d, 0, 0, 82, 3.2)
    poly(d, [pol(82, -90 + 120 * i) for i in range(3)], 3)
    poly(d, [pol(41, 45 + 90 * i) for i in range(4)], 2.6)
    circ(d, 0, 0, 22, 2.6, 80); circ(d, 0, 0, 6, 0, 255)

def e_life(d):
    circ(d, 0, -46, 28, 4)
    seg(d, (0, -18), (0, 86), 4.4); seg(d, (-42, 14), (42, 14), 4.4)
    for i in range(4): lens(d, -58 + i * 0, 0, 0, 0, 0, 0) if False else None
    poly(d, drop(-62, 56, 8, -140, 2.2), 2.4, True, fill=90)
    poly(d, drop(62, 56, 8, -40, 2.2), 2.4, True, fill=90)

def e_death(d):
    seg(d, (-52, 86), (28, -66), 5)
    poly(d, arc_pts(-34, -66, 62, -6, 78, 30), 5, False)
    poly(d, arc_pts(-34, -66, 52, 70, -4, 30), 2.4, False)
    for p in ((50, 40), (68, 10), (44, 70)): circ(d, p[0], p[1], 4, 0, 255)

def e_dragon(d):
    pts = arc_pts(0, 30, 78, 235, 305, 20) + arc_pts(0, -30, 78, 55, 125, 20)
    poly(d, pts, 3.6, True, fill=50)
    poly(d, ellipse_pts(11, 40, 0), 3, True, fill=220)
    for i in range(7):
        a = i * 50 - 150
        seg(d, pol(48, a), pol(64, a), 2.2)
    for i in range(5): circ(d, -54 + i * 27, 74, 8, 2.2, 60)

def e_void(d):
    for r, a0, a1 in ((78, 20, 300), (60, 100, 400), (42, 200, 500), (24, 300, 600)):
        poly(d, arc_pts(0, 0, r, a0, a1, 40), 3.2, False)
    circ(d, 0, 0, 9, 0, 255)

def e_creation(d):
    circ(d, 0, 0, 34, 2.8)
    for i in range(6): circ(d, *pol(34, i * 60), 34, 2.8)
    circ(d, 0, 0, 68, 3.2)
    circ(d, 0, 0, 6, 0, 255)

def e_eternity(d):
    pts = []
    for i in range(121):
        t = 2 * math.pi * i / 120
        den = 1 + math.sin(t) ** 2
        pts.append((82 * math.cos(t) / den, 82 * math.sin(t) * math.cos(t) / den))
    poly(d, pts, 4.2, True)
    circ(d, 0, 0, 5, 0, 255)

EMBLEMS2 = {n: globals()['e_' + n] for n in FRAMES2}

order = list(FRAMES2.keys())
for idx, name in enumerate(order):
    make_outer(name, 300 + idx)
    m, d = new(); frame_base(d); FRAMES2[name](d); finish(m, f'frame_{name}.png')
    m, d = new(); EMBLEMS2[name](d); finish(m, f'emblem_{name}.png', crop=100)
print('novos ok', len(order))
