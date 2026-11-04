# Camada "treliça" (lattice): linhas geometricas finas e ornamentadas, uma por elemento (44).
src = open('/home/claude/gen_circles.py', encoding='utf-8').read()
exec(src[:src.index('def f_fire')])      # helpers (seg, poly, circ, pol, spiral, finish...) + make_outer

RMAX = 236

def clip_circle(m, r=RMAX):
    mask = Image.new('L', (S, S), 0)
    ImageDraw.Draw(mask).ellipse([C - r*K, C - r*K, C + r*K, C + r*K], fill=255)
    return ImageChops.multiply(m, mask)

def base_rings(d, extra=()):
    for r, w in ((RMAX, 3), (RMAX - 9, 1.2)) + tuple(extra): circ(d, 0, 0, r, w)

def lat_star(d, n, layers):
    base_rings(d)
    for k in range(layers):
        r = RMAX - 14 - k * 38
        step = max(2, (n // 2) - k) if n > 4 else 2
        vs = [pol(r, -90 + 360.0 * i / n + 9 * k) for i in range(n)]
        for i in range(n): seg(d, vs[i], vs[(i + step) % n], 2.2)
        circ(d, 0, 0, r, 1.2)
    for i in range(n * 2): seg(d, pol(RMAX - 9, i * 180.0 / n), pol(RMAX - 20, i * 180.0 / n), 1.6)

def lat_rosette(d, n, layers=1):
    base_rings(d)
    for L_ in range(layers):
        ring = 120 - L_ * 30; rad = 118 - L_ * 18
        for i in range(n):
            c = pol(ring, 360.0 * i / n + L_ * 180.0 / n)
            circ(d, c[0], c[1], rad * (0.62 if n > 7 else 0.8), 1.8)
    circ(d, 0, 0, 52, 2.2); circ(d, 0, 0, 36, 1.2)
    for i in range(n): dot(d, pol(RMAX - 18, 360.0 * i / n), 4)

def lat_web(d, n, rings=5):
    base_rings(d)
    for i in range(n): seg(d, pol(34, 360.0 * i / n), pol(RMAX - 9, 360.0 * i / n), 1.6)
    for k in range(rings):
        r = 70 + k * (RMAX - 80) / max(1, rings - 1)
        poly(d, [pol(r, 360.0 * i / n + (180.0 / n) * (k % 2)) for i in range(n)], 2.0)
    for i in range(n): dot(d, pol(RMAX - 24, 360.0 * i / n + 180.0 / n), 3.5)

def lat_hex(d, spacing, tilt=0):
    base_rings(d)
    m2, d2 = new()
    for ang in (0, 60, 120):
        a = math.radians(ang + tilt); ca, sa = math.cos(a), math.sin(a)
        k = -8
        while k <= 8:
            off_ = k * spacing
            p1 = (-300 * ca - off_ * sa, -300 * sa + off_ * ca); p2 = (300 * ca - off_ * sa, 300 * sa + off_ * ca)
            seg(d2, p1, p2, 1.5)
            k += 1
    m2 = clip_circle(m2, RMAX - 14)
    d._image.paste(ImageChops.lighter(d._image, m2)) if hasattr(d, '_image') else None
    return m2

def lat_petal(d, n, layers):
    base_rings(d)
    for L_ in range(layers):
        r0 = 40 + L_ * 22; r1 = RMAX - 30 - L_ * 14
        for i in range(n):
            a = 360.0 * i / n + L_ * 180.0 / n
            pts = [pol(r0, a), pol((r0 + r1) / 2 + 10, a - 360.0 / n * 0.4), pol(r1, a), pol((r0 + r1) / 2 + 10, a + 360.0 / n * 0.4)]
            poly(d, pts, 1.9)
    circ(d, 0, 0, 34, 2)

def lat_mandala(d, n, layers=1):
    base_rings(d, ((190, 2), (150, 1.6), (110, 1.6), (70, 2)))
    for ring in (170, 130, 90):
        for i in range(n * 2):
            dot(d, pol(ring, 360.0 * i / (n * 2)), 3)
    for i in range(n):
        a = 360.0 * i / n
        poly(d, arc_pts(*pol(150, a), 40, a + 90, a + 270, 20), 1.8, False)
        seg(d, pol(70, a), pol(RMAX - 9, a), 1.4)

def lat_spiral(d, k, turns=300):
    base_rings(d, ((190, 1.4), (120, 1.4)))
    for j in range(k):
        poly(d, spiral(30, RMAX - 14, j * 360.0 / k, turns, 120), 2.2, False)
    for i in range(k * 4): dot(d, pol(RMAX - 24, 360.0 * i / (k * 4)), 3)

def lat_broken(d, seed):
    rng = random.Random(seed); base_rings(d)
    for r in (200, 160, 120, 80):
        a0 = rng.uniform(0, 360)
        while a0 < 360 + 1:
            span = rng.uniform(25, 80)
            poly(d, arc_pts(0, 0, r, a0, a0 + span, 24), 2.0, False); a0 += span + rng.uniform(8, 28)
    for _ in range(12):
        a = rng.uniform(0, 360); r = 40; pts = [pol(r, a)]
        for _ in range(5):
            r += rng.uniform(20, 36); a += rng.uniform(-8, 8); pts.append(pol(min(r, RMAX - 10), a))
        poly(d, pts, 1.8, False)

LAT = {
 'fire':('star',8,3), 'water':('rosette',6,2), 'lightning':('web',7,5), 'earth':('hex',44,0), 'wind':('petal',8,3), 'light':('star',12,3),
 'darkness':('mandala',5,1), 'metal':('hex',36,15), 'eclipse':('rosette',12,1), 'blood':('petal',6,2), 'celestial':('star',5,3),
 'gravity':('spiral',3,300), 'time':('web',12,6), 'reality_collapse':('broken',7,0),
 'ice':('star',12,2), 'nature':('petal',10,2), 'poison':('rosette',9,1), 'sand':('hex',52,30), 'steam':('mandala',6,1), 'magma':('star',7,3),
 'storm':('spiral',4,220), 'crystal':('hex',40,45), 'sound':('mandala',8,1), 'shadow':('web',5,4), 'acid':('rosette',7,2), 'glass':('broken',11,0),
 'sakura':('petal',5,3), 'bone':('web',8,3), 'spirit':('spiral',5,260), 'plasma':('star',9,3), 'dream':('mandala',7,1), 'nightmare':('web',9,6),
 'mirror':('hex',38,0), 'rune':('star',8,2), 'comet':('spiral',2,400), 'cosmos':('spiral',6,360), 'chaos':('star',11,4), 'order':('web',6,7),
 'life':('petal',12,3), 'death':('web',3,5), 'dragon':('star',6,3), 'void':('spiral',7,-300), 'creation':('rosette',12,2), 'eternity':('rosette',8,3),
}

for name, (fam, a, b) in LAT.items():
    m, d = new()
    if fam == 'star': lat_star(d, a, b)
    elif fam == 'rosette': lat_rosette(d, a, b)
    elif fam == 'web': lat_web(d, a, b)
    elif fam == 'hex':
        base_rings(d)
        m2, d2 = new()
        for ang in (0, 60, 120):
            ar = math.radians(ang + b); ca, sa = math.cos(ar), math.sin(ar)
            for k in range(-9, 10):
                off_ = k * a
                seg(d2, (-300 * ca - off_ * sa, -300 * sa + off_ * ca), (300 * ca - off_ * sa, 300 * sa + off_ * ca), 1.5)
        m = ImageChops.lighter(m, clip_circle(m2, RMAX - 14))
    elif fam == 'petal': lat_petal(d, a, b)
    elif fam == 'mandala': lat_mandala(d, a, b)
    elif fam == 'spiral': lat_spiral(d, a, b)
    elif fam == 'broken': lat_broken(d, 100 + a)
    finish(m, f'lattice_{name}.png')
print('lattices ok', len(LAT))
