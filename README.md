# Elemental Awakening (Forge 1.20.1)

Mod de magia inspirado no Roblox Elemental Awakening. Dados baseados na wiki de fas (podem estar errados).

## Como usar
1. Pegue o "Orbe do Despertar" (aba Ferramentas do criativo) e clique com o botao direito: gira e sorteia seu elemento (44 no total: 14 da wiki + 30 novos).
2. Teclas (mudaveis em Controles): Z X C V B N = habilidades 1 a 6, R = habilidade especial.
3. Cada elemento tem seu circulo magico (aparece ao despertar e nos golpes em area).

## Comandos (/aura ou /elemento)
/aura set <elemento>   escolhe seu elemento na hora (ex.: /aura set fogo  |  /aura set tempo  |  /aura set colapso)
/aura girar            sorteia um elemento, como o Orbe
/aura reset            remove seu elemento
/aura lista            mostra os nomes dos 14 elementos
/aura ver              mostra seu elemento atual
Aceita nome em portugues ou ingles (fogo/fire, agua/water, raio/lightning...). Aperta TAB para ver as sugestoes.
Item: /give @s elementalawakening:awakening_orb

## Habilidades
FIRE:      Z Fireball | X Eruption | C Fire Pulse | V Itto Kaso
WATER:     Z Water Blast | X Water Bomb | C Geyser | V Evaporation
LIGHTNING: Z Thunderbolt | X Discharge | C Stormcloud | V Lightning Cloak
EARTH:     Z Tremor | X Earth Wall | C Stoneskin | V Fissure  (passiva Earthbound: so no chao, dano maior)
WIND:      Z Gale | X Rising Wind | C Microburst | V High Pressure Bomb  (passiva Airbourne: 1.25x no ar)
LIGHT:     Z Lightbeam | X Beacon | C Wrath | V Stunning Radiance | R Blink
DARKNESS:  Z Expulsion | X Chains of Shadow | C Cull | V Soul Harvest | R Imbue (marca; golpes em marcados dao mais)
METAL:     Z Metal Blast | X Magnetism | C Spike Upheaval | V Metal Bombardment | R Metal Rod
ECLIPSE:   Z Lightfall | X Consecrate | C Lightblast | V Eclipse | R Ritual of Balance
BLOOD:     Z Transfusion | X Plague | C Blood Barrier | V Blood Manipulation | R Blood Sentinels
CELESTIAL: Z Solfire | X Planet Throw | C Lightspeed Lariat | V Supernova | B Harbinger | N Meteor Strike
GRAVITY:   Z Crush | X Push | C Skyhammer | V Rise | B Fall | N Pressure | R Flux  (passiva Singularity)
TIME:      Z Revert | X Temporal Shift | C Time Bomb | V Interdiction | B Fast Forward | N Timeline Destruction | R Timeline Divergence
REALITY_COLLAPSE: Z Sphere of Distortion | X Sphere of Fracture | C Sphere of Collapse | V Dissonance | B Traveler | N The Black Sun | R Collapse


## 30 ELEMENTOS NOVOS (criados para o mod) - 4 golpes cada (Z X C V)
Nao existem no jogo original. Cada um tem simbolo magico, estrutura 3D e particulas proprios.
UNCOMMON
 ICE (gelo):        Frost Shard | Blizzard | Frozen Nova | Glacier Spike
 NATURE:            Thorn Dart | Vine Trap | Photosynthesis (cura) | Seed Barrage
 POISON:            Venom Spit | Toxic Cloud | Contagion (pula entre inimigos) | Plague Burst
 SAND (areia):      Sand Bolt | Quicksand | Sandstorm | Dune Rush (investida)
 STEAM (vapor):     Steam Jet | Scalding Fog | Boiler Burst | Steam Rush (velocidade)
RARE
 MAGMA:             Magma Ball | Eruption Vent | Meteor Rain | Lava Pool
 STORM (tempestade):Bolt Strike | Thunderstorm | Gale Slam | Lightning Rain
 CRYSTAL (cristal): Shard Volley | Crystal Spire | Prism Burst | Crystal Armor
 SOUND (som):       Sonic Beam | Shockwave | Bass Field | Resonance Orb
 SHADOW (sombra):   Shadow Step | Dark Bolt | Umbral Field | Shade Blink (teleporte)
 ACID (acido):      Acid Spit | Corrosive Pool | Acid Rain | Dissolve Nova
 GLASS (vidro):     Glass Shard | Shatter | Refraction | Glass Guard
 SAKURA:            Petal Shot | Petal Storm | Blossom Field | Spring Breeze
EXOTIC
 BONE (osso):       Bone Dart | Bone Spikes | Rib Cage (prende) | Marrow Chain
 SPIRIT (espirito): Wisp | Phase Dash | Soul Vortex | Guardian Spirit (sentinela)
 PLASMA:            Plasma Lance | Plasma Orb | Ion Burst | Arc Surge
 DREAM (sonho):     Lullaby Orb | Dreamscape | Dream Step | Wake Shock
 NIGHTMARE:         Fear Bolt | Terror Field | Abyss Maw | Night Terror
 MIRROR (espelho):  Reflect Beam | Echo Strike | Mirror Burst | Reflection Step
 RUNE (runa):       Rune Bolt | Warding Circle | Glyph Rain | Runic Power (dano 1.5x)
 COMET (cometa):    Comet Dart | Comet Shower | Comet Rush | Impact Crater
LEGENDARY
 COSMOS:            Star Bolt | Gravity Well | Meteor Swarm | Nebula Burst
 CHAOS (caos):      Chaos Bolt | Entropy Rain | Disorder | Chaos Arc
 ORDER (ordem):     Decree | Law Field | Judgement | Balance
 LIFE (vida):       Life Seed | Rejuvenate (cura) | Growth Field | Bloom Burst
 DEATH (morte):     Reaper Bolt | Grave Mist | Scythe Dash | Death Knell
 DRAGON (dragao):   Dragon Breath | Wing Dive | Roar | Dragonfire Rain
CURSED LEGENDARY
 VOID (vazio):      Void Bolt | Event Horizon | Void Step | Null Burst
HEAVENLY
 CREATION (criacao):Genesis Orb | Rebirth | Spark of Creation | Genesis Core
 ETERNITY:          Infinity Ray | Stasis Field | Endless Spiral | Eternal Pulse
Use /aura set <nome> (ex.: /aura set dragao). Valores (dano, raio, cooldown) ficam na tabela no fim de GenAbilities.java.


## Animacoes cinematograficas
Golpes em area grandes (circulo >= 5 blocos) tocam uma sequencia propria do elemento (Cine.java):
  1) CARGA: o aro do simbolo se contrai para o centro + uma faisca cresce (0,3s)
  2) EXPLOSAO: tremor de camera, clarao colorido, zoom, estrutura brota com "pop"; golpes enormes (>= 8) ganham barras de cinema
  3) SUSTENTACAO: pecas orbitam, flutuam, caem em arco, convergem...
  4) DISSIPACAO: tudo some com fade
Cada um dos 44 elementos tem uma coreografia diferente. Golpes pequenos tocam uma versao curta.
Teste os golpes grandes (geralmente V) de cada elemento: /aura set <nome>.


## Circulos magicos (versao em camadas)
Cada elemento tem 4 texturas: aneis com runas (outer), TRELICA ornamentada (lattice, nova), figura geometrica (frame) e emblema.
No jogo o circulo tem profundidade: 3 camadas no chao girando em sentidos opostos + um segundo andar menor flutuando,
o emblema em pe girando como holograma e 6 runas em miniatura orbitando. Abre com giro rapido e "pop"; fecha encolhendo.
Ao conjurar qualquer golpe, o braco balanca e um circulo em pe se abre diante do personagem.
(tools_gen_lattice.py gera as trelicas)


## MAX CAST (segurar a tecla)
Golpes fortes (recarga >= 9s) e alguns especiais (Fireball, Thunderbolt, Meteor Strike) sao "de segurar" (marcados com » no HUD):
segure a tecla para carregar (barra no centro da tela), solte para disparar. A carga e medida no SERVIDOR (nao da pra forjar).
Carga maxima (~2,8s, solta sozinho em 4s) = mais dano (ate +80%), area maior (ate +40%) e circulos/cinematicas maiores, com recarga +50%.
Voce fica lento enquanto carrega. Golpes rapidos saem ao apertar.
Variantes de carga maxima proprias: Fireball (4 lasers convergentes), Thunderbolt (zona de relampagos),
Meteor Strike (meteoro gigante), Interdiction (fenda temporal e explosao enorme).

## HUD
Canto inferior esquerdo: elemento atual e uma barra por habilidade (tecla, nome, recarga em segundos).
Barra de carga do Max cast no centro da tela.

## 6 elementos novos a mais (6 golpes cada: Z X C V B N)
 SOLAR (Sol):  Sunbolt | Solar Ray | Flare | Scorching Field | Miniature Sun | Solar Eclipse Burst
 LUNAR (Lua):  Moonbeam Orb | Lunar Lance | Moonlit Field | Moon Step | Crescent Rain | Full Moon
 GOLD (Ouro):  Gold Bullet | Gilded Spire | Gold Rush | Golden Skin | Coin Storm | Midas Touch
 WEB (Teia):   Web Shot | Sticky Web | Silk Thread | Web Swing | Spider Nest | Cocoon
 RUST (Ferrugem): Rust Bolt | Corrosion Field | Oxidize | Scrap Rush | Scrap Rain | Decay Wave
 ECHO (Eco):   Echo Ping | Reverberation | Resonant Blast | Echo Step | Echo Clone | Infinite Echo
Os 30 elementos novos anteriores ganharam 2 golpes extras (teclas B e N): um utilitario e um supremo.
Total agora: 50 elementos.

## Simplificacoes (nao existem ainda)
- Max cast (carregar o golpe) e mana: nao existem. So cooldown (valores nossos, ajuste nos arquivos *Abilities.java).
- Custos de vida (Itto Kaso, Blood) funcionam. Ritual of Balance gasta fome no lugar de mana.

## Compilar pelo celular (Termux + GitHub Actions)
1. Crie um repositorio vazio no GitHub.
2. No Termux:
   pkg install git unzip
   unzip elementalawakening.zip && cd elementalawakening
   git init && git add . && git commit -m "mod"
   git branch -M main
   git remote add origin https://github.com/SEU_USUARIO/SEU_REPO.git
   git push -u origin main
   (use um Personal Access Token como senha)
3. GitHub > aba Actions > Build > baixe o artefato "elementalawakening-jar".
4. Coloque o .jar na pasta mods do Minecraft Java 1.20.1 com Forge 47.3.0.

As texturas dos circulos sao geradas por tools_gen_circles.py (Python + Pillow).
