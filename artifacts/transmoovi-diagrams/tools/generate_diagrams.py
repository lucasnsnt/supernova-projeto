#!/usr/bin/env python3
"""Generate editable draw.io sources and matching SVG/PNG/PDF exports."""
from pathlib import Path
from xml.sax.saxutils import escape
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
EDIT = ROOT / "editaveis"
SVG = ROOT / "svg"
PNG = ROOT / "png"
PDF = ROOT / "pdf"
for p in (EDIT, SVG, PNG, PDF): p.mkdir(parents=True, exist_ok=True)

COLORS = {
    "ink":"#172033", "muted":"#596579", "line":"#98A2B3", "bg":"#F6F8FC", "white":"#FFFFFF",
    "blue":"#2563EB", "blue2":"#EAF1FF", "cyan":"#0891B2", "cyan2":"#E6F8FC",
    "green":"#15803D", "green2":"#EAF8EF", "amber":"#B45309", "amber2":"#FFF4E5",
    "purple":"#7C3AED", "purple2":"#F2ECFF", "red":"#C2413B", "red2":"#FDECEC",
    "slate":"#475467", "slate2":"#EEF1F5"
}
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

def font(size, bold=False): return ImageFont.truetype(FONT_B if bold else FONT, size)

class Diagram:
    def __init__(self, name, title, subtitle, w=1800, h=1100):
        self.name,self.title,self.subtitle,self.w,self.h=name,title,subtitle,w,h
        self.nodes=[]; self.edges=[]; self.groups=[]; self.notes=[]
    def group(self,x,y,w,h,title,color="blue"):
        self.groups.append((x,y,w,h,title,color)); return self
    def node(self,id,x,y,w,h,title,lines=(),color="blue",kind="box"):
        self.nodes.append(dict(id=id,x=x,y=y,w=w,h=h,title=title,lines=list(lines),color=color,kind=kind)); return self
    def edge(self,a,b,label="",style="solid",color="line",card_a="",card_b=""):
        self.edges.append(dict(a=a,b=b,label=label,style=style,color=color,card_a=card_a,card_b=card_b)); return self
    def note(self,x,y,w,h,title,lines=(),color="slate"):
        self.notes.append(dict(x=x,y=y,w=w,h=h,title=title,lines=list(lines),color=color)); return self

def center(n): return n['x']+n['w']/2,n['y']+n['h']/2
def anchor(a,b):
    ax,ay=center(a); bx,by=center(b); dx,dy=bx-ax,by-ay
    if abs(dx/a['w']) > abs(dy/a['h']):
        return (a['x']+a['w'] if dx>0 else a['x'], ay), (b['x'] if dx>0 else b['x']+b['w'], by)
    return (ax, a['y']+a['h'] if dy>0 else a['y']), (bx, b['y'] if dy>0 else b['y']+b['h'])

def svg_text(x,y,text,size=16,weight=400,fill=None,anchor="start"):
    fill=fill or COLORS['ink']; return f'<text x="{x}" y="{y}" font-family="DejaVu Sans,Arial" font-size="{size}" font-weight="{weight}" fill="{fill}" text-anchor="{anchor}">{escape(str(text))}</text>'

def render_svg(d):
    out=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{d.w}" height="{d.h}" viewBox="0 0 {d.w} {d.h}">',
         '<defs><filter id="shadow" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="3" stdDeviation="5" flood-opacity=".12"/></filter><marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto"><path d="M0,0 L0,6 L9,3 z" fill="#667085"/></marker></defs>',
         f'<rect width="100%" height="100%" fill="{COLORS["bg"]}"/>',svg_text(60,58,d.title,30,700),svg_text(60,88,d.subtitle,15,400,COLORS['muted'])]
    for x,y,w,h,t,c in d.groups:
        out += [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="18" fill="{COLORS[c+"2"]}" fill-opacity=".48" stroke="{COLORS[c]}" stroke-opacity=".35" stroke-width="2"/>',svg_text(x+20,y+30,t.upper(),13,700,COLORS[c])]
    byid={n['id']:n for n in d.nodes}
    for e in d.edges:
        a,b=byid[e['a']],byid[e['b']]; p1,p2=anchor(a,b); dash=' stroke-dasharray="8 6"' if e['style']=='dash' else ''
        out.append(f'<path d="M {p1[0]} {p1[1]} L {p2[0]} {p2[1]}" fill="none" stroke="{COLORS[e["color"]]}" stroke-width="2"{dash} marker-end="url(#arrow)"/>')
        mx,my=(p1[0]+p2[0])/2,(p1[1]+p2[1])/2
        if e['label']: out += [f'<rect x="{mx-60}" y="{my-15}" width="120" height="22" rx="7" fill="{COLORS["white"]}"/>',svg_text(mx,my+2,e['label'],12,400,COLORS['muted'],'middle')]
        if e['card_a']: out.append(svg_text(p1[0]+7,p1[1]-7,e['card_a'],12,700,COLORS['ink']))
        if e['card_b']: out.append(svg_text(p2[0]-7,p2[1]-7,e['card_b'],12,700,COLORS['ink'],'end'))
    for n in d.nodes:
        c=n['color']; x,y,w,h=n['x'],n['y'],n['w'],n['h']
        if n['kind']=='decision':
            pts=f'{x+w/2},{y} {x+w},{y+h/2} {x+w/2},{y+h} {x},{y+h/2}'
            out.append(f'<polygon points="{pts}" fill="{COLORS[c+"2"]}" stroke="{COLORS[c]}" stroke-width="2" filter="url(#shadow)"/>')
            out.append(svg_text(x+w/2,y+h/2+5,n['title'],15,700,COLORS['ink'],'middle')); continue
        out.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="{COLORS["white"]}" stroke="{COLORS[c]}" stroke-width="2" filter="url(#shadow)"/>')
        out.append(f'<rect x="{x}" y="{y}" width="8" height="{h}" rx="4" fill="{COLORS[c]}"/>')
        out.append(svg_text(x+22,y+28,n['title'],16,700,COLORS['ink']))
        yy=y+52
        for line in n['lines']:
            out.append(svg_text(x+22,yy,line,12,400,COLORS['muted'])); yy+=18
    for n in d.notes:
        c=n['color']; x,y,w,h=n['x'],n['y'],n['w'],n['h']
        out.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="{COLORS[c+"2"]}" stroke="{COLORS[c]}" stroke-dasharray="6 4"/>')
        out.append(svg_text(x+18,y+27,n['title'],14,700,COLORS[c])); yy=y+49
        for line in n['lines']: out.append(svg_text(x+18,yy,line,12,400,COLORS['ink'])); yy+=17
    out.append('</svg>'); (SVG/f'{d.name}.svg').write_text('\n'.join(out),encoding='utf-8')

def render_png(d):
    im=Image.new('RGB',(d.w,d.h),COLORS['bg']); dr=ImageDraw.Draw(im)
    dr.text((60,30),d.title,font=font(30,1),fill=COLORS['ink']); dr.text((60,68),d.subtitle,font=font(15),fill=COLORS['muted'])
    for x,y,w,h,t,c in d.groups:
        dr.rounded_rectangle((x,y,x+w,y+h),18,fill=COLORS[c+'2'],outline=COLORS[c],width=2); dr.text((x+20,y+12),t.upper(),font=font(13,1),fill=COLORS[c])
    byid={n['id']:n for n in d.nodes}
    for e in d.edges:
        p1,p2=anchor(byid[e['a']],byid[e['b']]); dr.line((*p1,*p2),fill=COLORS[e['color']],width=3)
        import math
        ang=math.atan2(p2[1]-p1[1],p2[0]-p1[0]); q1=(p2[0]-14*math.cos(ang-.45),p2[1]-14*math.sin(ang-.45)); q2=(p2[0]-14*math.cos(ang+.45),p2[1]-14*math.sin(ang+.45)); dr.polygon([p2,q1,q2],fill=COLORS[e['color']])
        if e['label']:
            mx,my=(p1[0]+p2[0])/2,(p1[1]+p2[1])/2; box=dr.textbbox((0,0),e['label'],font=font(12)); tw=box[2]
            dr.rounded_rectangle((mx-tw/2-7,my-14,mx+tw/2+7,my+7),6,fill=COLORS['white']); dr.text((mx-tw/2,my-12),e['label'],font=font(12),fill=COLORS['muted'])
        if e['card_a']: dr.text((p1[0]+5,p1[1]-18),e['card_a'],font=font(12,1),fill=COLORS['ink'])
        if e['card_b']: dr.text((p2[0]-25,p2[1]-18),e['card_b'],font=font(12,1),fill=COLORS['ink'])
    for n in d.nodes:
        x,y,w,h,c=n['x'],n['y'],n['w'],n['h'],n['color']
        if n['kind']=='decision':
            dr.polygon([(x+w/2,y),(x+w,y+h/2),(x+w/2,y+h),(x,y+h/2)],fill=COLORS[c+'2'],outline=COLORS[c]);
            bb=dr.textbbox((0,0),n['title'],font=font(15,1)); dr.text((x+w/2-(bb[2]-bb[0])/2,y+h/2-8),n['title'],font=font(15,1),fill=COLORS['ink']); continue
        dr.rounded_rectangle((x,y,x+w,y+h),12,fill=COLORS['white'],outline=COLORS[c],width=2); dr.rounded_rectangle((x,y,x+8,y+h),4,fill=COLORS[c]); dr.text((x+22,y+12),n['title'],font=font(16,1),fill=COLORS['ink'])
        yy=y+43
        for line in n['lines']: dr.text((x+22,yy),line,font=font(12),fill=COLORS['muted']); yy+=18
    for n in d.notes:
        x,y,w,h,c=n['x'],n['y'],n['w'],n['h'],n['color']; dr.rounded_rectangle((x,y,x+w,y+h),12,fill=COLORS[c+'2'],outline=COLORS[c],width=2); dr.text((x+18,y+10),n['title'],font=font(14,1),fill=COLORS[c]); yy=y+39
        for line in n['lines']: dr.text((x+18,yy),line,font=font(12),fill=COLORS['ink']); yy+=17
    im.save(PNG/f'{d.name}.png',optimize=True)

def render_drawio(d):
    mx=ET.Element('mxfile',host='app.diagrams.net',modified='2026-09-18T00:00:00.000Z',agent='Codex',version='24.7.17',type='device')
    dia=ET.SubElement(mx,'diagram',id=d.name,name=d.title); model=ET.SubElement(dia,'mxGraphModel',dx='1600',dy='900',grid='1',gridSize='10',guides='1',tooltips='1',connect='1',arrows='1',fold='1',page='1',pageScale='1',pageWidth=str(d.w),pageHeight=str(d.h),math='0',shadow='0')
    root=ET.SubElement(model,'root'); ET.SubElement(root,'mxCell',id='0'); ET.SubElement(root,'mxCell',id='1',parent='0')
    idx=2
    def cell(value,style,x,y,w,h,parent='1',vertex=True):
        nonlocal idx; cid=str(idx); idx+=1; c=ET.SubElement(root,'mxCell',id=cid,value=value,style=style,parent=parent,vertex='1' if vertex else '0'); ET.SubElement(c,'mxGeometry',x=str(x),y=str(y),width=str(w),height=str(h),**{'as':'geometry'}); return cid
    cell(f'<b>{escape(d.title)}</b><br><font color="{COLORS["muted"]}">{escape(d.subtitle)}</font>',f'text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=top;fontSize=24;fontColor={COLORS["ink"]};',55,25,d.w-110,70)
    for x,y,w,h,t,c in d.groups: cell(f'<b>{escape(t.upper())}</b>',f'rounded=1;whiteSpace=wrap;html=1;verticalAlign=top;align=left;spacingTop=10;spacingLeft=14;fillColor={COLORS[c+"2"]};fillOpacity=45;strokeColor={COLORS[c]};dashed=1;fontColor={COLORS[c]};',x,y,w,h)
    ids={}
    for n in d.nodes:
        val='<b>'+escape(n['title'])+'</b>' + (('<br><font color="'+COLORS['muted']+'">'+'<br>'.join(escape(x) for x in n['lines'])+'</font>') if n['lines'] else '')
        shape='rhombus;' if n['kind']=='decision' else 'rounded=1;'
        ids[n['id']]=cell(val,f'{shape}whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacing=12;fillColor={COLORS["white"]};strokeColor={COLORS[n["color"]]};strokeWidth=2;fontColor={COLORS["ink"]};fontSize=13;',n['x'],n['y'],n['w'],n['h'])
    for n in d.notes:
        val='<b>'+escape(n['title'])+'</b><br>'+'<br>'.join(escape(x) for x in n['lines']); cell(val,f'rounded=1;whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacing=10;fillColor={COLORS[n["color"]+"2"]};strokeColor={COLORS[n["color"]]};dashed=1;fontColor={COLORS["ink"]};',n['x'],n['y'],n['w'],n['h'])
    for e in d.edges:
        c=ET.SubElement(root,'mxCell',id=str(idx),value=escape(e['label']),style=f'edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;endArrow=block;endFill=1;strokeColor={COLORS[e["color"]]};dashed={1 if e["style"]=="dash" else 0};fontSize=11;labelBackgroundColor={COLORS["white"]};',parent='1',source=ids[e['a']],target=ids[e['b']],edge='1'); idx+=1; ET.SubElement(c,'mxGeometry',relative='1',**{'as':'geometry'})
    ET.indent(mx); ET.ElementTree(mx).write(EDIT/f'{d.name}.drawio',encoding='utf-8',xml_declaration=True)

def architecture():
    d=Diagram('01-arquitetura-geral','Transmoovi — arquitetura geral','Contexto lógico, integrações e implantação observados no repositório',1800,1120)
    d.group(40,110,330,780,'Experiência','purple').group(400,110,850,780,'Aplicação','blue').group(1280,110,480,780,'Serviços externos','cyan')
    d.node('users',85,175,240,135,'Usuários',('Aluno','Motorista','Administrador'),'purple').node('pwa',85,380,240,150,'Web / PWA',('React + TypeScript','Vite • Service Worker','Google Maps JS'),'purple')
    d.node('nginx',450,175,260,120,'Entrada HTTPS',('Nginx / API pública','CORS + cookies seguros'),'slate').node('api',450,365,300,190,'API Spring Boot',('Controllers REST','JWT + CSRF','Serviços transacionais','Scheduler a cada minuto'),'blue')
    d.node('domain',850,345,330,230,'Domínio de transporte',('Cadastro e vínculos','Rotas recorrentes','Confirmações diárias','Planejamento e viagens','Tracking e notificações'),'blue')
    d.node('db',620,680,300,145,'PostgreSQL / H2 local',('Flyway V1–V8','JPA repositories','Histórico preservado'),'green').node('sse',980,680,220,145,'Notificações',('Persistência in-app','SSE em tempo real'),'amber')
    d.node('ors',1330,185,360,140,'OpenRouteService',('Geocodificação Pelias','Backend • chave privada'),'cyan').node('google',1330,390,360,155,'Google Route Optimization',('OAuth conta de serviço','Capacidade + janelas','Polyline + ordem de paradas'),'cyan').node('maps',1330,650,360,130,'Google Maps JavaScript',('Mapa de endereço','Mapa da viagem ativa'),'cyan')
    for a,b,l in [('users','pwa','interage'),('pwa','nginx','HTTPS / JSON'),('nginx','api','/api'),('api','domain','orquestra'),('domain','db','JPA'),('domain','sse','eventos'),('domain','ors','geocodifica'),('domain','google','otimiza'),('pwa','maps','renderiza')]: d.edge(a,b,l)
    d.note(70,930,800,120,'Implantação de produção',('Frontend na Vercel; backend em container na VPS.','API em 127.0.0.1:8081 atrás do Nginx; banco não exposto à internet.'),'slate')
    d.note(930,930,800,120,'Legenda',('Azul: aplicação/domínio • Roxo: cliente • Verde: dados','Ciano: integração externa • Tracejado: observação/contexto'),'slate'); return d

def database():
    d=Diagram('02-modelo-de-dados','Transmoovi — modelo de dados','Esquema efetivo após Flyway V1–V8 • PK/FK/UQ e cardinalidades principais',2200,1650)
    d.group(35,105,680,460,'Identidade e cadastro','purple').group(745,105,680,460,'Oferta e matrícula','blue').group(1455,105,710,460,'Segurança e comunicação','slate').group(35,600,1320,995,'Operação diária e viagem','green').group(1390,600,775,995,'Referências geográficas','cyan')
    # identity
    d.node('address',75,175,285,235,'addresses',('PK id','street, number, neighborhood','city, state, zip_code','latitude, longitude'),'cyan')
    d.node('user',395,155,285,270,'users',('PK user_id','UQ email','FK/UQ address_id → addresses','name, password, phone','date_of_birth, role'),'purple')
    d.node('driver',395,455,285,175,'drivers',('PK/FK user_id → users','UQ cnh','FK operational_address_id','status, status_reason'),'purple')
    d.node('student',75,455,285,155,'students',('PK/FK user_id → users','FK institution_id → institutions'),'purple')
    # offer
    d.node('vehicle',785,155,270,190,'vehicles',('PK id','FK driver_id → drivers','UQ license_plate','passenger_capacity','default_vehicle'),'blue')
    d.node('route',1090,155,290,185,'recurring_routes',('PK id','FK driver_id → drivers','FK vehicle_id → vehicles','name, active, created_at'),'blue')
    d.node('sched',785,385,270,205,'recurring_route_schedules',('PK id','FK route_id → recurring_routes','day_of_week, direction','departure_time','response_deadline_time'),'blue')
    d.node('routeinst',1090,385,290,210,'recurring_route_institutions',('PK id','FK route_id → recurring_routes','FK institution_id → institutions','UQ route + institution','stop_order, times'),'blue')
    d.node('enroll',1090,635,290,215,'recurring_route_enrollments',('PK id','FK route_id → recurring_routes','FK student_id → students','UQ route + student','directions, status'),'blue')
    d.node('studentsched',785,635,270,190,'student_schedules',('PK id','FK student_id → students','UQ student/day/time/direction','academic time'),'blue')
    d.node('institution',1485,645,300,180,'institutions',('PK id','FK/UQ address_id → addresses','name, institution_type'),'cyan')
    d.node('link',785,865,270,190,'driver_student_links',('PK id','FK driver_id → drivers','FK student_id → students','start/end, status'),'blue')
    d.node('invite',1485,855,300,195,'driver_invite',('PK id','FK driver_id → drivers','UQ token','expires_at, status'),'slate')
    # security
    d.node('refresh',1490,160,300,190,'refresh_tokens',('PK id','FK user_id → users','UQ token_hash','expires_at, status'),'slate')
    d.node('emailv',1830,160,300,210,'email_verifications',('PK id','UQ email','UQ registration_token_hash','code_hash, expiry, attempts'),'slate')
    d.node('notif',1830,410,300,225,'in_app_notifications',('PK id','FK recipient_id → users','FK trip_id → trips','FK confirmation_id → confirmations','type, message, read_at'),'slate')
    # operation
    d.node('confirm',75,690,320,285,'daily_confirmations',('PK id • version','FK driver_id → drivers','FK student_id → students','FK recurring_route_id → routes','UQ student/date/direction/route/time','status, deadlines, academic_time'),'green')
    d.node('trip',430,1040,350,320,'trips',('PK id • version','FK driver_id → drivers','FK vehicle_id → vehicles','FK recurring_route_id → routes','UQ route/date/direction','status + lifecycle timestamps','provider, reference, polyline','completed_stop_count'),'green')
    d.node('participant',75,1110,320,240,'trip_participants',('PK id','FK trip_id → trips','FK student_id → students','FK/UQ confirmation_id → confirmations','UQ trip + student','orders + ETAs'),'green')
    d.node('location',835,1110,300,205,'trip_locations',('PK/FK trip_id → trips','latitude, longitude','accuracy, heading','recorded_at, updated_at'),'green')
    rel=[('user','address','0..1','0..1'),('driver','user','1','1'),('student','user','1','1'),('driver','address','0..1','0..*'),('vehicle','driver','0..*','1'),('route','driver','0..*','1'),('route','vehicle','0..*','1'),('sched','route','0..*','1'),('routeinst','route','0..*','1'),('routeinst','institution','0..*','1'),('enroll','route','0..*','1'),('enroll','student','0..*','1'),('studentsched','student','0..*','1'),('student','institution','0..*','0..1'),('institution','address','1','1'),('link','driver','0..*','1'),('link','student','0..*','1'),('invite','driver','0..*','1'),('refresh','user','0..*','1'),('notif','user','0..*','1'),('notif','trip','0..*','0..1'),('notif','confirm','0..*','0..1'),('confirm','driver','0..*','1'),('confirm','student','0..*','1'),('confirm','route','0..*','0..1'),('trip','driver','0..*','1'),('trip','vehicle','0..*','0..1'),('trip','route','0..*','0..1'),('participant','trip','1..*','1'),('participant','student','0..*','1'),('participant','confirm','0..1','1'),('location','trip','0..1','1')]
    for a,b,ca,cb in rel: d.edge(a,b,'',color='line',card_a=ca,card_b=cb)
    d.note(1420,1110,685,250,'Legenda relacional',('PK = chave primária • FK = chave estrangeira • UQ = unicidade','1 = obrigatório e único • 0..1 = opcional • 0..* = muitos','Setas apontam para a entidade referenciada.','Domínios são agrupamentos de leitura; não são schemas PostgreSQL.'),'slate')
    d.note(1420,1400,685,135,'Nota de histórico',('V8 preserva confirmações/viagens históricas, liga ocorrências inequívocas','a rotas recorrentes e mantém casos ambíguos em NEEDS_ATTENTION.'),'amber'); return d

def geolocation():
    d=Diagram('03-fluxo-geolocalizacao','Transmoovi — geolocalização e rota','Do endereço digitado ao rastreamento ao vivo, com validações e persistência',2000,1250)
    d.group(35,115,360,990,'Cliente','purple').group(425,115,850,990,'Backend','blue').group(1305,115,650,990,'Provedores e dados','cyan')
    d.node('form',80,175,270,140,'1. Cadastro de endereço',('Rua, número, bairro','cidade, UF e CEP'),'purple').node('preview',80,380,270,135,'2. Prévia no mapa',('POST /api/geocoding/preview','AddressMap / Google Maps'),'purple')
    d.node('save',80,620,270,145,'3. Salvar perfil',('Alteração textual limpa','coordenadas antigas'),'purple').node('gps',80,865,270,155,'8. GPS do motorista',('watchPosition no navegador','lat/lng/accuracy/heading'),'purple')
    d.node('controller',470,185,310,135,'GeocodingController',('Valida AddressRequest','delega preview'),'blue').node('geo',870,170,350,180,'GeocodingService',('Só resolve coordenadas ausentes','confidence ≥ 0,6 • país BR','limites válidos de lat/lng'),'blue')
    d.node('planning',470,500,360,210,'4. TripPlanningService',('Base operacional do motorista','casas + instituições','capacidade e horário acadêmico','IDA: casa → instituição','VOLTA: instituição → casa'),'blue')
    d.node('gateway',875,520,345,190,'5. RoutePlanningGateway',('shipment por confirmação YES','janelas de tempo + capacidade','saída fixa'),'blue').node('tracking',470,855,350,170,'9. TripTrackingService',('Aceita somente IN_PROGRESS','upsert por trip_id','aluno lê apenas se participa'),'blue')
    d.node('ors',1370,180,500,155,'OpenRouteService / Pelias',('GET /pelias/v1/search • Brasil • pt','retorna longitude, latitude e confiança'),'cyan').node('addresses',1370,410,500,145,'addresses',('latitude/longitude NUMERIC(38,8)','usuário, instituição e base operacional'),'green')
    d.node('google',1370,655,500,175,'Google Route Optimization API',('optimizeTours • OAuth conta de serviço','ordem de coleta/entrega, ETAs e polyline'),'cyan').node('tripdata',1370,890,500,155,'trips + trip_locations',('rota congelada no início','última posição sobrescrita (1:1)','visível somente durante a viagem'),'green')
    for a,b,l in [('form','preview','preenche'),('preview','controller','POST preview'),('controller','geo','preview'),('geo','ors','busca'),('ors','geo','coordenadas'),('geo','preview','lat/lng'),('preview','save','confirma'),('geo','addresses','persistência pelo chamador'),('save','addresses','salva dados'),('addresses','planning','pontos'),('planning','gateway','request'),('gateway','google','optimizeTours'),('google','tripdata','polyline + paradas'),('gps','tracking','PUT localização'),('tracking','tripdata','upsert')]: d.edge(a,b,l)
    d.note(70,1060,870,115,'Falhas previstas',('Geocodificação desabilitada, baixa confiança ou indisponibilidade bloqueiam prévia/planejamento.','Sem coordenadas ou rota viável, a viagem fica NEEDS_ATTENTION; confirmados não são removidos.'),'red')
    d.note(1010,1060,870,115,'Legenda',('Roxo: navegador • Azul: serviços internos • Ciano: provedor externo • Verde: persistência','Linha contínua: chamada/fluxo de dados. Numeração indica a sequência principal.'),'slate'); return d

def operation():
    d=Diagram('04-operacao-da-viagem','Transmoovi — operação da viagem','Rota fixa → confirmação → otimização → início → rastreamento → conclusão',2100,1320)
    lanes=[('Configuração',115,'purple'),('Ocorrência diária',370,'blue'),('Planejamento',625,'amber'),('Execução',880,'green')]
    for title,y,c in lanes: d.group(35,y,2025,210,title,c)
    d.node('route',80,165,260,120,'Motorista cria rota',('dia/direção/saída','instituições + veículo'),'purple').node('enroll',400,165,260,120,'Aluno entra na rota',('convite ativo','instituição atendida'),'purple').node('agenda',720,165,260,120,'Agenda acadêmica',('valida chegada/coleta','não cria viagem sozinha'),'purple')
    d.node('release',80,420,280,125,'Scheduler libera confirmação',('hoje e amanhã','fuso America/Bahia'),'blue').node('answer',420,420,250,125,'Aluno responde',('YES ou NO','até responseDeadline'),'blue').node('expire',730,420,250,125,'Prazo expira',('PENDING → NO_RESPONSE'),'blue').node('freezeq',1040,420,280,125,'Conjunto elegível',('somente YES vigente','matrícula/vínculo válidos'),'blue')
    d.node('build',80,675,280,130,'Construir ocorrência',('uma viagem por rota/data/direção','veículo da rota'),'amber').node('validate',430,675,250,130,'Validar',('capacidade, endereços','horário acadêmico'),'amber').node('opt',750,675,300,130,'Otimizar Google',('janelas + geografia','ordem, ETAs, polyline'),'amber').node('feasible',1140,670,230,140,'Rota viável?',(), 'amber','decision').node('planned',1450,675,250,130,'PLANNED',('manifesto e rota prontos','notifica participantes'),'green').node('attention',1450,835,250,130,'NEEDS_ATTENTION',('preserva confirmados','permite recalcular'),'red')
    d.node('preview',80,930,275,125,'Motorista abre prévia',('recalcula se não terminal','pode iniciar cedo'),'green').node('window',430,925,250,140,'Dentro de ±30 min?',(), 'green','decision').node('ack',760,930,280,125,'Confirma exceção',('“Iniciar antecipadamente”','ou “Iniciar agora”'),'amber').node('start',1120,930,260,130,'IN_PROGRESS',('congela manifesto','PENDING → NO_RESPONSE'),'green').node('track',1450,930,250,130,'Rastreamento',('posição 1:1 da viagem','concluir paradas'),'green').node('complete',1770,930,250,130,'COMPLETED',('completed_at','posição deixa de aparecer'),'green')
    edges=[('route','enroll','publica'),('enroll','agenda','exige perfil'),('agenda','release','dia elegível'),('release','answer','PENDING'),('answer','freezeq','YES'),('expire','freezeq','exclui'),('freezeq','build','após prazo / prévia'),('build','validate',''),('validate','opt','dados válidos'),('opt','feasible','resultado'),('feasible','planned','sim'),('feasible','attention','não'),('planned','preview','dia da saída'),('attention','opt','replanejar'),('preview','window','iniciar'),('window','start','sim'),('window','ack','não'),('ack','start','confirmado'),('start','track','GPS'),('track','complete','concluir')]
    for a,b,l in edges: d.edge(a,b,l)
    d.note(70,1145,930,110,'Guardas de início',('Motorista aprovado • nenhuma outra viagem IN_PROGRESS • ocorrência não vazia • rota viável','Fora da janela inclusiva de 30 min antes/depois, é obrigatório acknowledge explícito.'),'red')
    d.note(1060,1145,930,110,'Legenda de estados',('PENDING → YES | NO | NO_RESPONSE • PLANNED → IN_PROGRESS → COMPLETED','PLANNING/PLANNED podem resultar em NEEDS_ATTENTION; viagem também pode ser CANCELLED.'),'slate'); return d

def main():
    diagrams=[architecture(),database(),geolocation(),operation()]
    for d in diagrams: render_svg(d); render_png(d); render_drawio(d)
    pages=[]
    for d in diagrams:
        im=Image.open(PNG/f'{d.name}.png').convert('RGB'); im.thumbnail((2480,1754)); canvas=Image.new('RGB',(2480,1754),'white'); canvas.paste(im,((2480-im.width)//2,(1754-im.height)//2)); pages.append(canvas)
    pages[0].save(PDF/'transmoovi-diagramas.pdf',save_all=True,append_images=pages[1:],resolution=150.0)
    print(f'Generated {len(diagrams)} diagrams in {ROOT}')

if __name__ == '__main__': main()
