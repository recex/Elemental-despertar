# Texturas dos 6 elementos novos: solar, lunar, gold, web, rust, echo (outer, frame, emblem, lattice)
src_new = open('/home/claude/gen_new.py', encoding='utf-8').read()
exec(src_new[:src_new.index('def e_ice')])            # helpers + fr_* + lens/cloud/star4
src_lat = open('/home/claude/gen_lattice.py', encoding='utf-8').read()
exec(src_lat[:src_lat.index('LAT = {')])              # lat_* + clip_circle + base_rings

# ---------------- emblemas
def e_solar(d):
    circ(d, 0, 0, 30, 3.6, 150); circ(d, 0, 0, 40, 2)
    for i in range(16):
        a = i * 22.5
        poly(d, [pol(48, a - 5), pol(88 if i % 2 == 0 else 68, a), pol(48, a + 5)], 2.6, True, fill=90)
    for i in range(8): circ(d, *pol(98, i * 45 + 22.5), 3, 0, 255)

def e_lunar(d):
    pts = arc_pts(0, 0, 70, 50, 310, 60) + arc_pts(26, 0, 54, 295, 65, 60)
    poly(d, pts, 3.6, True, fill=55)
    circ(d, -34, -18, 8, 2.4); circ(d, -44, 14, 5, 2.2); circ(d, -22, 32, 6, 2.2)
    star4(d, 52, -26, 12); star4(d, 70, 18, 8)

def e_gold(d):
    circ(d, 0, 0, 78, 4, 40); circ(d, 0, 0, 64, 2)
    star4(d, 0, 0, 44, 3)
    for i in range(12): circ(d, *pol(71, i * 30), 3, 0, 255)
    poly(d, [(0, -28), (8, -8), (28, 0), (8, 8), (0, 28), (-8, 8), (-28, 0), (-8, -8)], 2.2, True, fill=160)

def e_web(d):
    for i in range(8): seg(d, (0, 0), pol(86, i * 45), 2.6)
    for r in (22, 40, 58, 76):
        pts = []
        for i in range(8):
            a = i * 45; b = (i + 1) * 45
            pts.append(pol(r, a))
            m = pol(r * 0.88, (a + b) / 2)
            pts.append(m)
        poly(d, pts, 2.2)
    circ(d, 0, 0, 7, 2.4, 255)

def e_rust(d):
    pts = []
    for i in range(10):
        a = i * 36
        for da, r in ((-11, 56), (-7, 76), (7, 76), (11, 56)): pts.append(pol(r, a + da))
    poly(d, pts, 3.4, True, fill=55)
    circ(d, 0, 0, 24, 3.2)
    seg(d, (-40, -20), (-10, 8), 2.2); seg(d, (-10, 8), (-24, 36), 2.2); seg(d, (30, -44), (6, -8), 2.2); seg(d, (6, -8), (30, 24), 2.2)
    poly(d, drop(-60, 60, 8, -90, 2.2), 2.4, True, fill=90)
    poly(d, drop(54, 66, 6, -90, 2.2), 2.2, True, fill=90)

def e_echo(d):
    circ(d, 0, 0, 9, 0, 255)
    for k, r in enumerate((24, 42, 60, 78)):
        a0 = 20 + k * 25
        poly(d, arc_pts(0, 0, r, a0, a0 + 130, 30), 3.0, False)
        poly(d, arc_pts(0, 0, r, a0 + 180, a0 + 310, 30), 3.0, False)

EM = {'solar': e_solar, 'lunar': e_lunar, 'gold': e_gold, 'web': e_web, 'rust': e_rust, 'echo': e_echo}

# ---------------- frames
FR = {
 'solar': lambda d: (fr_rays(d, 24, 106, 168), fr_star(d, 12, 5, 150), fr_dots(d, 12, 120, 4)),
 'lunar': lambda d: (fr_orbits(d, 3, 160, 60), fr_dots(d, 16, 140, 3), fr_star(d, 5, 2, 128)),
 'gold':  lambda d: (fr_polys(d, [6, 6], 168), fr_gear(d, 12, 132, 150), fr_dots(d, 12, 118, 4)),
 'web':   lambda d: (fr_lattice(d, 8), fr_polys(d, [8, 8], 150)),
 'rust':  lambda d: (fr_gear(d, 14, 140, 162), fr_cracks(d, 21, 8), fr_dots(d, 7, 118, 5)),
 'echo':  lambda d: (circ(d, 0, 0, 120, 2.4), circ(d, 0, 0, 138, 2.4), circ(d, 0, 0, 156, 2.4), fr_rays(d, 24, 108, 160, 2)),
}

# ---------------- trelicas
LAT2 = {'solar': ('rosette', 10, 3), 'lunar': ('rosette', 10, 2), 'gold': ('hex', 46, 20),
        'web': ('web', 8, 7), 'rust': ('hex', 34, 10), 'echo': ('mandala', 9, 1)}

for idx, name in enumerate(EM):
    make_outer(name, 500 + idx)
    m, d = new(); frame_base(d); FR[name](d); finish(m, f'frame_{name}.png')
    m, d = new(); EM[name](d); finish(m, f'emblem_{name}.png', crop=100)
    fam, a, b = LAT2[name]
    m, d = new()
    if fam == 'rosette': lat_rosette(d, a, b)
    elif fam == 'web': lat_web(d, a, b)
    elif fam == 'mandala': lat_mandala(d, a, b)
    elif fam == 'hex':
        base_rings(d)
        m2, d2 = new()
        for ang in (0, 60, 120):
            ar = math.radians(ang + b); ca, sa = math.cos(ar), math.sin(ar)
            for k in range(-9, 10):
                off_ = k * a
                seg(d2, (-300 * ca - off_ * sa, -300 * sa + off_ * ca), (300 * ca - off_ * sa, 300 * sa + off_ * ca), 1.5)
        m = ImageChops.lighter(m, clip_circle(m2, RMAX - 14))
    finish(m, f'lattice_{name}.png')
print('extras ok')
