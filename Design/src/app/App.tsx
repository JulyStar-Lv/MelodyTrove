import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "motion/react";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import {
  Play, Pause, SkipForward, SkipBack, Heart,
  Search, Home, Library, Settings, Music2,
  ChevronUp, ChevronRight, ChevronLeft, ChevronDown,
  Volume2, VolumeX, Shuffle, Repeat,
  MoreHorizontal, Download, Share2, Sun, Moon,
  List, Grid3x3, Plus, X, Bell,
  Disc3, Headphones, Speaker, Wifi, Bluetooth,
  Database, Cloud, HardDrive, ArrowLeft, ArrowRight,
  Zap, SlidersHorizontal, Folder, Globe, Server,
  RefreshCw, AlertCircle, Star, Bookmark, Mic,
  Activity, Palette, Smartphone, Tablet, Monitor,
  Radio, Package, BarChart2, Sparkles, ListMusic,
  Hash, Layers, CheckCircle2, Gauge, Filter,
  LayoutDashboard, Code2, Maximize2, Minimize2,
  PanelRight, PanelRightClose, AlignLeft, Cpu,
  Plug, Puzzle, FileText, GitBranch, Terminal,
  TrendingUp, Clock, Heart as HeartIcon, Star as StarIcon,
  FolderOpen, Wifi as WifiIcon, Music, Mic2
} from "lucide-react";

const cn = (...i: unknown[]) => twMerge(clsx(i));

// ─────────────────────────────────────────────────────────────
// TYPES
// ─────────────────────────────────────────────────────────────
interface Song { id: number; title: string; artist: string; album: string; duration: string; gradient: [string,string]; liked: boolean; quality?: "lossless"|"hi-res"|"dolby"|"standard"; }
interface Album { id: number; title: string; artist: string; year: number; gradient: [string,string]; tracks: number; genre: string; }
interface Artist { id: number; name: string; followers: string; gradient: [string,string]; genre: string; initials: string; }
interface Playlist { id: number; title: string; description: string; gradient: [string,string]; tracks: number; duration: string; }
type Page = "cover"|"home"|"search"|"library"|"settings"|"design-system";
type DSSection = "cover"|"foundation"|"tokens"|"components"|"patterns"|"compose";
type LibTab = "songs"|"albums"|"artists"|"genres"|"folders"|"playlists"|"favorites"|"downloads"|"history"|"recently-added"|"recently-played"|"lossless"|"hi-res"|"sources";
type RightPanel = "lyrics"|"queue"|null;

// ─────────────────────────────────────────────────────────────
// MOCK DATA
// ─────────────────────────────────────────────────────────────
const G: [string,string][] = [
  ["#FF5B8A","#7A6CFF"],["#7A6CFF","#3D9AFF"],["#FF8A3D","#FF5B8A"],
  ["#3DCA8A","#3D9AFF"],["#FFD93D","#FF8A3D"],["#3D9AFF","#7A6CFF"],
  ["#FF5B8A","#FF8A3D"],["#7A6CFF","#3DCA8A"],
];

const SONGS: Song[] = [
  { id:1, title:"Midnight Cascade", artist:"Luna Waves", album:"Tidal Drift", duration:"3:42", gradient:G[0], liked:true, quality:"hi-res" },
  { id:2, title:"Neon Undertow", artist:"Prism Circuit", album:"Voltage Dreams", duration:"4:18", gradient:G[1], liked:false, quality:"lossless" },
  { id:3, title:"Silver Tide", artist:"Coastal Drift", album:"Open Water", duration:"3:55", gradient:G[2], liked:true, quality:"standard" },
  { id:4, title:"Aurora Sequence", artist:"Polar Echo", album:"Northern Lights", duration:"5:02", gradient:G[3], liked:false, quality:"dolby" },
  { id:5, title:"Depth Protocol", artist:"Ocean Syntax", album:"Subsonic", duration:"3:30", gradient:G[4], liked:true },
  { id:6, title:"Glass Architecture", artist:"Fractal Mind", album:"Prism", duration:"4:44", gradient:G[5], liked:false, quality:"lossless" },
  { id:7, title:"Resonance Fields", artist:"Wave Function", album:"Quantum", duration:"3:15", gradient:G[6], liked:true },
  { id:8, title:"Liminal Space", artist:"Threshold", album:"Between", duration:"5:30", gradient:G[7], liked:false, quality:"hi-res" },
];
const ALBUMS: Album[] = [
  { id:1, title:"Tidal Drift", artist:"Luna Waves", year:2024, gradient:G[0], tracks:12, genre:"Electronic" },
  { id:2, title:"Voltage Dreams", artist:"Prism Circuit", year:2024, gradient:G[1], tracks:9, genre:"Synthwave" },
  { id:3, title:"Open Water", artist:"Coastal Drift", year:2023, gradient:G[2], tracks:11, genre:"Ambient" },
  { id:4, title:"Northern Lights", artist:"Polar Echo", year:2024, gradient:G[3], tracks:8, genre:"IDM" },
  { id:5, title:"Subsonic", artist:"Ocean Syntax", year:2023, gradient:G[4], tracks:14, genre:"Techno" },
  { id:6, title:"Glass Architecture", artist:"Fractal Mind", year:2024, gradient:G[5], tracks:10, genre:"Post-Rock" },
  { id:7, title:"Quantum", artist:"Wave Function", year:2024, gradient:G[6], tracks:7, genre:"Experimental" },
  { id:8, title:"Between", artist:"Threshold", year:2023, gradient:G[7], tracks:13, genre:"Shoegaze" },
];
const ARTISTS: Artist[] = [
  { id:1, name:"Luna Waves", followers:"2.4M", gradient:G[0], genre:"Electronic", initials:"LW" },
  { id:2, name:"Prism Circuit", followers:"1.8M", gradient:G[1], genre:"Synthwave", initials:"PC" },
  { id:3, name:"Coastal Drift", followers:"890K", gradient:G[2], genre:"Ambient", initials:"CD" },
  { id:4, name:"Polar Echo", followers:"3.1M", gradient:G[3], genre:"IDM", initials:"PE" },
  { id:5, name:"Ocean Syntax", followers:"670K", gradient:G[4], genre:"Techno", initials:"OS" },
  { id:6, name:"Fractal Mind", followers:"1.2M", gradient:G[5], genre:"Post-Rock", initials:"FM" },
];
const PLAYLISTS: Playlist[] = [
  { id:1, title:"Evening Frequencies", description:"Deep electronic for golden hour", gradient:G[0], tracks:24, duration:"1h 32m" },
  { id:2, title:"Spatial Audio Mix", description:"Hi-Res Dolby Atmos collection", gradient:G[1], tracks:18, duration:"1h 08m" },
  { id:3, title:"Deep Focus", description:"Minimal ambient for concentration", gradient:G[2], tracks:32, duration:"2h 15m" },
  { id:4, title:"Night Drive", description:"Synthwave for late-night cruising", gradient:G[3], tracks:20, duration:"1h 22m" },
  { id:5, title:"Sunrise Protocol", description:"Gentle morning electronic", gradient:G[4], tracks:16, duration:"58m" },
  { id:6, title:"System Override", description:"High-energy techno and industrial", gradient:G[5], tracks:28, duration:"1h 45m" },
];
const LYRICS_LINES = [
  "In the deep blue hours before the dawn",
  "The frequency shifts as the night moves on",
  "Cascading waves of midnight light",
  "Your signal cutting through the night",
  "",
  "Midnight cascade, falling through the sound",
  "Midnight cascade, where frequencies are found",
  "Every waveform bends to your will",
  "The ocean of sound, perfectly still",
  "",
  "I hear you in the reverb",
  "In the echo of the void",
  "A signal from the deep",
  "That cannot be destroyed",
];

// ─────────────────────────────────────────────────────────────
// PRIMITIVE COMPONENTS
// ─────────────────────────────────────────────────────────────
function QualityBadge({ quality }: { quality?: string }) {
  if (!quality || quality === "standard") return null;
  const c = { "hi-res": { l:"Hi-Res", color:"#FFD93D" }, lossless:{ l:"Lossless", color:"#3DCA8A" }, dolby:{ l:"Dolby Atmos", color:"#3D9AFF" } };
  const cfg = c[quality as keyof typeof c]; if (!cfg) return null;
  return (
    <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-bold tracking-wide font-mono shrink-0"
      style={{ background:`${cfg.color}20`, color:cfg.color, border:`1px solid ${cfg.color}40` }}>{cfg.l}</span>
  );
}

function Btn({ children, variant="filled", size="md", className="", onClick, icon, iconOnly }: {
  children?: React.ReactNode; variant?:"filled"|"outlined"|"ghost"|"tonal"|"secondary"; size?:"sm"|"md"|"lg";
  className?:string; onClick?:()=>void; icon?:React.ReactNode; iconOnly?:boolean;
}) {
  const sz = { sm:"h-8 text-xs px-3 gap-1.5", md:"h-10 text-sm px-4 gap-2", lg:"h-12 text-base px-6 gap-2.5" };
  const isz = { sm:"h-8 w-8", md:"h-10 w-10", lg:"h-12 w-12" };
  const v = {
    filled:"bg-primary text-primary-foreground hover:opacity-90 active:scale-95",
    outlined:"border border-border text-foreground hover:bg-muted active:scale-95",
    ghost:"text-foreground hover:bg-muted active:scale-95",
    tonal:"bg-muted text-foreground hover:bg-muted/80 active:scale-95",
    secondary:"bg-secondary text-secondary-foreground hover:opacity-90 active:scale-95",
  };
  return (
    <button onClick={onClick} className={cn("inline-flex items-center justify-center font-semibold rounded-full transition-all duration-150 shrink-0 select-none",
      iconOnly ? isz[size] : sz[size], v[variant], className)}>
      {icon && <span className={iconOnly?"":"shrink-0"}>{icon}</span>}
      {!iconOnly && children}
    </button>
  );
}

function TideSwitch({ checked, onChange, label }: { checked:boolean; onChange:(v:boolean)=>void; label?:string }) {
  return (
    <label className="flex items-center gap-3 cursor-pointer select-none">
      {label && <span className="text-sm text-foreground">{label}</span>}
      <button role="switch" aria-checked={checked} onClick={()=>onChange(!checked)}
        className={cn("relative w-12 h-7 rounded-full transition-all duration-300 shrink-0", checked?"bg-primary":"bg-switch-background")}>
        <motion.div layout transition={{ type:"spring", stiffness:700, damping:35 }}
          className="absolute top-1 w-5 h-5 bg-white rounded-full shadow-md"
          style={{ left: checked ? "calc(100% - 24px)" : "4px" }} />
      </button>
    </label>
  );
}

function TideSlider({ value, onChange, label, accent }: { value:number; onChange:(v:number)=>void; label?:string; accent?:string }) {
  const pct = value;
  return (
    <div className="flex flex-col gap-2 w-full">
      {label && <span className="text-xs text-muted-foreground font-medium">{label}</span>}
      <div className="relative h-5 flex items-center w-full group cursor-pointer">
        <div className="absolute inset-x-0 h-1.5 bg-muted rounded-full overflow-hidden">
          <div className="h-full rounded-full transition-all" style={{ width:`${pct}%`, background:accent||"var(--tide-pink)" }} />
        </div>
        <input type="range" min={0} max={100} value={value} onChange={e=>onChange(Number(e.target.value))}
          className="absolute inset-0 w-full opacity-0 cursor-pointer h-full" />
        <div className="absolute w-5 h-5 bg-white rounded-full shadow-md border-2 transition-transform group-hover:scale-110"
          style={{ left:`calc(${pct}% - 10px)`, borderColor:accent||"var(--tide-pink)" }} />
      </div>
    </div>
  );
}

function PillTabs({ tabs, active, onChange }: { tabs:{id:string;label:string}[]; active:string; onChange:(id:string)=>void }) {
  return (
    <div className="flex items-center gap-2 overflow-x-auto pb-1 hide-scrollbar">
      {tabs.map(t => (
        <button key={t.id} onClick={()=>onChange(t.id)}
          className={cn("shrink-0 px-4 h-9 rounded-full text-sm font-semibold transition-all",
            active===t.id?"bg-primary text-primary-foreground":"bg-muted text-muted-foreground hover:text-foreground")}>
          {t.label}
        </button>
      ))}
    </div>
  );
}

function SegTabs({ tabs, active, onChange }: { tabs:{id:string;label:string}[]; active:string; onChange:(id:string)=>void }) {
  return (
    <div className="inline-flex bg-muted rounded-2xl p-1 gap-1">
      {tabs.map(t => (
        <button key={t.id} onClick={()=>onChange(t.id)}
          className={cn("relative px-4 h-9 rounded-xl text-sm font-semibold transition-all",
            active===t.id?"text-foreground":"text-muted-foreground hover:text-foreground")}>
          {active===t.id && <motion.div layoutId="seg-bg" className="absolute inset-0 bg-card rounded-xl shadow-sm" />}
          <span className="relative z-10">{t.label}</span>
        </button>
      ))}
    </div>
  );
}

function UnderlineTabs({ tabs, active, onChange }: { tabs:{id:string;label:string}[]; active:string; onChange:(id:string)=>void }) {
  return (
    <div className="flex items-center border-b border-border">
      {tabs.map(t => (
        <button key={t.id} onClick={()=>onChange(t.id)}
          className={cn("relative px-4 py-3 text-sm font-semibold transition-all", active===t.id?"text-primary":"text-muted-foreground hover:text-foreground")}>
          {t.label}
          {active===t.id && <motion.div layoutId="tab-line" className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary rounded-full" />}
        </button>
      ))}
    </div>
  );
}

function SectionHeader({ title, action, onAction }: { title:string; action?:string; onAction?:()=>void }) {
  return (
    <div className="flex items-center justify-between mb-4">
      <h2 className="text-lg font-bold text-foreground">{title}</h2>
      {action && <button onClick={onAction} className="text-sm font-semibold text-primary hover:opacity-80 transition-opacity flex items-center gap-1">{action} <ChevronRight className="w-3.5 h-3.5" /></button>}
    </div>
  );
}

function SkeletonBlock({ className="" }: { className?:string }) {
  return <div className={cn("bg-muted rounded-2xl animate-pulse", className)} />;
}

function EmptyState({ icon, title, subtitle, action, onAction }: { icon:React.ReactNode; title:string; subtitle?:string; action?:string; onAction?:()=>void }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-8 text-center gap-4">
      <div className="w-16 h-16 rounded-3xl bg-muted flex items-center justify-center text-muted-foreground">{icon}</div>
      <div><p className="font-semibold text-foreground mb-1">{title}</p>{subtitle&&<p className="text-sm text-muted-foreground">{subtitle}</p>}</div>
      {action&&<Btn variant="tonal" onClick={onAction}>{action}</Btn>}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// MUSIC CARD COMPONENTS
// ─────────────────────────────────────────────────────────────
function AlbumCard({ album, size="md", onClick }: { album:Album; size?:"sm"|"md"|"lg"; onClick?:()=>void }) {
  const s = { sm:{a:120,w:"w-[120px]"}, md:{a:160,w:"w-[160px]"}, lg:{a:200,w:"w-[200px]"} }[size];
  return (
    <motion.div whileHover={{y:-4,scale:1.02}} whileTap={{scale:0.97}} transition={{type:"spring",stiffness:400,damping:30}}
      onClick={onClick} className={cn("shrink-0 cursor-pointer group",s.w)}>
      <div className="rounded-3xl overflow-hidden shadow-lg mb-3 relative" style={{width:s.a,height:s.a,background:`linear-gradient(135deg,${album.gradient[0]},${album.gradient[1]})`}}>
        <div className="absolute inset-0 flex items-center justify-center opacity-20 group-hover:opacity-30 transition-opacity"><Disc3 className="w-16 h-16 text-white"/></div>
        <div className="absolute inset-0 bg-gradient-to-t from-black/30 to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-end justify-end p-3">
          <div className="w-9 h-9 bg-white/95 rounded-full flex items-center justify-center shadow-lg"><Play className="w-4 h-4 ml-0.5" style={{color:album.gradient[0]}}/></div>
        </div>
      </div>
      <p className="text-sm font-semibold text-foreground truncate">{album.title}</p>
      <p className="text-xs text-muted-foreground truncate mt-0.5">{album.artist} · {album.year}</p>
    </motion.div>
  );
}

function ArtistCard({ artist, onClick }: { artist:Artist; onClick?:()=>void }) {
  return (
    <motion.div whileHover={{y:-4,scale:1.02}} whileTap={{scale:0.97}} transition={{type:"spring",stiffness:400,damping:30}}
      onClick={onClick} className="shrink-0 w-[128px] cursor-pointer group text-center">
      <div className="w-[128px] h-[128px] rounded-full overflow-hidden shadow-lg mb-3 relative mx-auto flex items-center justify-center"
        style={{background:`linear-gradient(135deg,${artist.gradient[0]},${artist.gradient[1]})`}}>
        <span className="text-3xl font-bold text-white/90 select-none">{artist.initials}</span>
        <div className="absolute inset-0 rounded-full bg-black/0 group-hover:bg-black/10 transition-colors"/>
      </div>
      <p className="text-sm font-semibold text-foreground truncate">{artist.name}</p>
      <p className="text-xs text-muted-foreground mt-0.5">{artist.followers}</p>
    </motion.div>
  );
}

function PlaylistCard({ playlist, onClick }: { playlist:Playlist; onClick?:()=>void }) {
  return (
    <motion.div whileHover={{y:-4,scale:1.02}} whileTap={{scale:0.97}} transition={{type:"spring",stiffness:400,damping:30}}
      onClick={onClick} className="shrink-0 w-[160px] cursor-pointer group">
      <div className="w-[160px] h-[160px] rounded-3xl overflow-hidden shadow-lg mb-3 relative flex items-center justify-center"
        style={{background:`linear-gradient(135deg,${playlist.gradient[0]},${playlist.gradient[1]})`}}>
        <ListMusic className="w-14 h-14 text-white/30 group-hover:text-white/50 transition-colors"/>
        <div className="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent flex items-end p-3">
          <p className="text-xs text-white/80 font-medium">{playlist.tracks} tracks · {playlist.duration}</p>
        </div>
        <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
          <div className="w-9 h-9 bg-white/95 rounded-full flex items-center justify-center shadow-lg"><Play className="w-4 h-4 ml-0.5" style={{color:playlist.gradient[0]}}/></div>
        </div>
      </div>
      <p className="text-sm font-semibold text-foreground truncate">{playlist.title}</p>
      <p className="text-xs text-muted-foreground mt-0.5 truncate">{playlist.description}</p>
    </motion.div>
  );
}

function MusicCard({ song, onPlay, isPlaying }: { song:Song; onPlay:(s:Song)=>void; isPlaying?:boolean }) {
  return (
    <motion.div whileHover={{scale:1.01}} whileTap={{scale:0.98}} transition={{type:"spring",stiffness:400,damping:30}}
      onClick={()=>onPlay(song)}
      className={cn("flex items-center gap-4 p-3.5 rounded-2xl cursor-pointer transition-colors group",isPlaying?"bg-primary/10 border border-primary/20":"hover:bg-muted/60")}>
      <div className="w-11 h-11 rounded-xl flex items-center justify-center shrink-0 relative"
        style={{background:`linear-gradient(135deg,${song.gradient[0]},${song.gradient[1]})`}}>
        {isPlaying ? (
          <div className="flex items-end gap-0.5 h-4">
            {[1,2,3,4].map(i=>(
              <motion.div key={i} className="w-1 bg-white rounded-full"
                animate={{height:["40%","100%","60%","80%"]}}
                transition={{duration:0.8,repeat:Infinity,delay:i*0.1,ease:"easeInOut"}}/>
            ))}
          </div>
        ) : (
          <>
            <Music2 className="w-4 h-4 text-white/80 group-hover:hidden"/>
            <Play className="w-4 h-4 text-white hidden group-hover:block ml-0.5"/>
          </>
        )}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <p className={cn("text-sm font-semibold truncate",isPlaying?"text-primary":"text-foreground")}>{song.title}</p>
          <QualityBadge quality={song.quality}/>
        </div>
        <p className="text-xs text-muted-foreground truncate mt-0.5">{song.artist} · {song.album}</p>
      </div>
      <div className="flex items-center gap-2 shrink-0">
        <span className="text-xs text-muted-foreground font-mono">{song.duration}</span>
        <button className={cn("opacity-0 group-hover:opacity-100 transition-opacity",song.liked?"opacity-100":"")}>
          <Heart className={cn("w-4 h-4",song.liked?"fill-primary text-primary":"text-muted-foreground")}/>
        </button>
      </div>
    </motion.div>
  );
}

function SourceCard({ source }: { source:{name:string;type:string;icon:React.ReactNode;status:"connected"|"syncing"|"error"|"idle";storage:string;tracks:number;gradient:[string,string]} }) {
  const sc = { connected:{l:"Connected",c:"var(--tide-green)"}, syncing:{l:"Syncing",c:"var(--tide-blue)"}, error:{l:"Error",c:"#FF4F4F"}, idle:{l:"Idle",c:"var(--muted-foreground)"} }[source.status];
  return (
    <div className="bg-card rounded-3xl p-5 border border-border hover:border-primary/30 transition-all group">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl flex items-center justify-center text-white" style={{background:`linear-gradient(135deg,${source.gradient[0]},${source.gradient[1]})`}}>{source.icon}</div>
          <div><p className="font-semibold text-foreground text-sm">{source.name}</p><p className="text-xs text-muted-foreground">{source.type}</p></div>
        </div>
        <div className="flex items-center gap-1.5"><div className="w-1.5 h-1.5 rounded-full" style={{background:sc.c}}/><span className="text-xs font-medium" style={{color:sc.c}}>{sc.l}</span></div>
      </div>
      <div className="grid grid-cols-2 gap-2 mb-4">
        <div className="bg-muted rounded-xl p-3"><p className="text-[10px] text-muted-foreground mb-0.5">Storage</p><p className="text-sm font-semibold text-foreground">{source.storage}</p></div>
        <div className="bg-muted rounded-xl p-3"><p className="text-[10px] text-muted-foreground mb-0.5">Tracks</p><p className="text-sm font-semibold text-foreground">{source.tracks.toLocaleString()}</p></div>
      </div>
      <div className="flex gap-2">
        <Btn variant="tonal" size="sm" icon={<RefreshCw className="w-3.5 h-3.5"/>} className="flex-1">Sync</Btn>
        <Btn variant="ghost" size="sm" icon={<FileText className="w-3.5 h-3.5"/>} iconOnly/>
        <Btn variant="ghost" size="sm" icon={<SlidersHorizontal className="w-3.5 h-3.5"/>} iconOnly/>
        <Btn variant="ghost" size="sm" icon={<MoreHorizontal className="w-3.5 h-3.5"/>} iconOnly/>
      </div>
    </div>
  );
}

function SettingItem({ label, subtitle, leading, trailing, onClick, danger }: { label:string; subtitle?:string; leading?:React.ReactNode; trailing?:React.ReactNode; onClick?:()=>void; danger?:boolean }) {
  return (
    <button onClick={onClick} className="w-full flex items-center gap-4 px-4 py-3.5 hover:bg-muted/50 transition-colors text-left group">
      {leading && <div className="w-9 h-9 rounded-xl bg-muted flex items-center justify-center shrink-0 text-muted-foreground">{leading}</div>}
      <div className="flex-1 min-w-0">
        <p className={cn("text-sm font-medium",danger?"text-destructive":"text-foreground")}>{label}</p>
        {subtitle&&<p className="text-xs text-muted-foreground mt-0.5">{subtitle}</p>}
      </div>
      {trailing!==undefined ? trailing : <ChevronRight className="w-4 h-4 text-muted-foreground shrink-0"/>}
    </button>
  );
}

function SettingsCard({ title, children }: { title:string; children:React.ReactNode }) {
  return (
    <div className="mb-5">
      <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest px-1 mb-2">{title}</p>
      <div className="bg-card rounded-3xl border border-border overflow-hidden divide-y divide-border/60">{children}</div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// COVER PAGE (APP)
// ─────────────────────────────────────────────────────────────
function CoverPage({ onEnter }: { onEnter: ()=>void }) {
  const principles = ["Simple","Calm","Immersive","Music First","Content First","Adaptive","Native","Cross Platform","Plugin Driven"];
  return (
    <div className="relative min-h-full flex flex-col items-center justify-center px-8 py-16 overflow-hidden">
      {/* Ambient BG */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-[-20%] left-[-10%] w-[60%] h-[60%] rounded-full opacity-20 blur-3xl" style={{background:`radial-gradient(circle,${G[0][0]},transparent)`}}/>
        <div className="absolute bottom-[-20%] right-[-10%] w-[60%] h-[60%] rounded-full opacity-20 blur-3xl" style={{background:`radial-gradient(circle,${G[1][1]},transparent)`}}/>
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[40%] h-[40%] rounded-full opacity-10 blur-3xl" style={{background:`radial-gradient(circle,${G[2][0]},transparent)`}}/>
      </div>

      {/* Logo */}
      <motion.div initial={{opacity:0,y:20}} animate={{opacity:1,y:0}} transition={{duration:0.7,ease:"easeOut"}}
        className="flex flex-col items-center gap-6 mb-16 relative z-10">
        <div className="w-20 h-20 rounded-[28px] flex items-center justify-center shadow-2xl"
          style={{background:"linear-gradient(135deg,var(--tide-pink),var(--tide-purple))",boxShadow:`0 20px 60px ${G[0][0]}60`}}>
          <Music2 className="w-10 h-10 text-white"/>
        </div>
        <div className="text-center">
          <h1 className="text-5xl font-black tracking-tight" style={{background:`linear-gradient(135deg,${G[0][0]},${G[1][1]})`,WebkitBackgroundClip:"text",WebkitTextFillColor:"transparent",backgroundClip:"text"}}>
            TideTunes
          </h1>
          <p className="text-lg text-muted-foreground mt-2 font-medium">One Library. Every Source.</p>
        </div>
      </motion.div>

      {/* Principles */}
      <motion.div initial={{opacity:0,y:24}} animate={{opacity:1,y:0}} transition={{duration:0.7,delay:0.15,ease:"easeOut"}}
        className="flex flex-wrap justify-center gap-2.5 max-w-lg mb-12 relative z-10">
        {principles.map((p,i)=>(
          <motion.div key={p} initial={{opacity:0,scale:0.85}} animate={{opacity:1,scale:1}} transition={{duration:0.4,delay:0.2+i*0.06,type:"spring",stiffness:400,damping:25}}
            className="px-4 py-2 rounded-full text-sm font-semibold border border-border bg-card/60 text-foreground backdrop-blur-sm hover:border-primary/40 hover:text-primary transition-all cursor-default">
            {p}
          </motion.div>
        ))}
      </motion.div>

      {/* Enter button */}
      <motion.div initial={{opacity:0,y:16}} animate={{opacity:1,y:0}} transition={{duration:0.6,delay:0.8}} className="relative z-10">
        <motion.button whileHover={{scale:1.04}} whileTap={{scale:0.96}}
          onClick={onEnter}
          className="flex items-center gap-3 px-8 h-14 rounded-full text-white font-bold text-base shadow-xl transition-shadow hover:shadow-2xl"
          style={{background:`linear-gradient(135deg,${G[0][0]},${G[0][1]})`,boxShadow:`0 8px 32px ${G[0][0]}60`}}>
          <Play className="w-5 h-5 fill-white"/> Enter TideTunes
        </motion.button>
      </motion.div>

      {/* Version */}
      <p className="absolute bottom-8 text-xs text-muted-foreground font-mono">TideTunes Design System · v3.0 · 2024</p>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// PLAYER COMPONENTS
// ─────────────────────────────────────────────────────────────
function MiniPlayer({ song, isPlaying, onPlayPause, onNext, onExpand }: { song:Song|null; isPlaying:boolean; onPlayPause:()=>void; onNext:()=>void; onExpand:()=>void }) {
  if (!song) return null;
  return (
    <motion.div initial={{y:80,opacity:0}} animate={{y:0,opacity:1}} exit={{y:80,opacity:0}} transition={{type:"spring",stiffness:400,damping:35}} className="mx-3 mb-2">
      <div className="relative flex items-center gap-3 px-4 h-[68px] rounded-[28px] cursor-pointer overflow-hidden"
        style={{background:"rgba(22,18,36,0.85)",backdropFilter:"blur(40px) saturate(180%)",WebkitBackdropFilter:"blur(40px) saturate(180%)",border:"1px solid var(--border)",boxShadow:"0 8px 32px rgba(0,0,0,0.3)"}}
        onClick={onExpand}>
        <div className="absolute inset-0 opacity-[0.08]" style={{background:`linear-gradient(90deg,${song.gradient[0]},${song.gradient[1]})`}}/>
        <div className="w-11 h-11 rounded-xl shrink-0 relative z-10 flex items-center justify-center" style={{background:`linear-gradient(135deg,${song.gradient[0]},${song.gradient[1]})`}}>
          {isPlaying ? <div className="flex items-end gap-0.5 h-3.5">{[1,2,3].map(i=><motion.div key={i} className="w-0.5 bg-white rounded-full" animate={{height:["30%","100%","60%"]}} transition={{duration:0.7,repeat:Infinity,delay:i*0.15,ease:"easeInOut"}}/>)}</div>
            : <Music2 className="w-4 h-4 text-white/80"/>}
        </div>
        <div className="flex-1 min-w-0 relative z-10">
          <p className="text-sm font-semibold text-foreground truncate">{song.title}</p>
          <p className="text-xs text-muted-foreground truncate">{song.artist}</p>
        </div>
        <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-border">
          <motion.div className="h-full rounded-full" style={{background:`linear-gradient(90deg,${song.gradient[0]},${song.gradient[1]})`}}
            animate={{width:isPlaying?"65%":"40%"}} transition={{duration:isPlaying?5:0,repeat:isPlaying?Infinity:0,ease:"linear"}}/>
        </div>
        <div className="flex items-center gap-1 relative z-10" onClick={e=>e.stopPropagation()}>
          <button onClick={onPlayPause} className="w-10 h-10 rounded-full flex items-center justify-center hover:bg-white/10 transition-colors active:scale-90">
            {isPlaying?<Pause className="w-5 h-5 text-white fill-white"/>:<Play className="w-5 h-5 text-white fill-white ml-0.5"/>}
          </button>
          <button onClick={onNext} className="w-10 h-10 rounded-full flex items-center justify-center hover:bg-white/10 transition-colors active:scale-90">
            <SkipForward className="w-5 h-5 text-white/90"/>
          </button>
        </div>
      </div>
    </motion.div>
  );
}

function FullPlayer({ song, isPlaying, onPlayPause, onNext, onPrev, onClose, progress, onSeek, volume, onVolume }: {
  song:Song; isPlaying:boolean; onPlayPause:()=>void; onNext:()=>void; onPrev:()=>void;
  onClose:()=>void; progress:number; onSeek:(v:number)=>void; volume:number; onVolume:(v:number)=>void;
}) {
  const [liked,setLiked] = useState(song.liked);
  const [activeTab,setActiveTab] = useState<"lyrics"|"queue"|"eq">("lyrics");
  const [shuffle,setShuffle] = useState(false);
  const [repeat,setRepeat] = useState(false);
  return (
    <motion.div initial={{y:"100%"}} animate={{y:0}} exit={{y:"100%"}} transition={{type:"spring",stiffness:300,damping:35}} className="fixed inset-0 z-50 flex flex-col overflow-hidden">
      <div className="absolute inset-0">
        <div className="absolute inset-0" style={{background:`linear-gradient(160deg,${song.gradient[0]}60,${song.gradient[1]}40,#0C0A14 60%)`}}/>
        <div className="absolute inset-0 backdrop-blur-3xl"/>
        <div className="absolute inset-0 bg-background/80"/>
      </div>
      <div className="relative z-10 flex flex-col h-full max-w-lg mx-auto w-full px-6 pt-2">
        {/* Header */}
        <div className="flex items-center justify-between py-4">
          <button onClick={onClose} className="w-10 h-10 rounded-full bg-muted/60 flex items-center justify-center"><ChevronDown className="w-5 h-5"/></button>
          <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">Now Playing</p>
          <button className="w-10 h-10 rounded-full bg-muted/60 flex items-center justify-center"><MoreHorizontal className="w-5 h-5"/></button>
        </div>
        {/* Art */}
        <motion.div className="mx-auto mb-6" animate={{scale:isPlaying?1:0.88}} transition={{type:"spring",stiffness:200,damping:25}}>
          <div className="w-64 h-64 rounded-[40px] shadow-2xl relative flex items-center justify-center"
            style={{background:`linear-gradient(135deg,${song.gradient[0]},${song.gradient[1]})`,boxShadow:`0 20px 60px ${song.gradient[0]}60`}}>
            <Disc3 className="w-24 h-24 text-white/20"/>
            {isPlaying&&<div className="absolute bottom-5 flex items-end gap-1 h-6">{[1,2,3,4,5].map(i=><motion.div key={i} className="w-1 bg-white/70 rounded-full" animate={{height:[`${20+i*5}%`,"100%",`${30+i*4}%`]}} transition={{duration:0.6+i*0.1,repeat:Infinity,delay:i*0.08}}/>)}</div>}
          </div>
        </motion.div>
        {/* Info */}
        <div className="flex items-center gap-4 mb-5">
          <div className="flex-1 min-w-0">
            <h2 className="text-2xl font-bold text-foreground truncate">{song.title}</h2>
            <div className="flex items-center gap-2 mt-1"><p className="text-base text-muted-foreground">{song.artist}</p><QualityBadge quality={song.quality}/></div>
          </div>
          <button onClick={()=>setLiked(!liked)} className="w-11 h-11 rounded-full flex items-center justify-center active:scale-90 transition-all">
            <Heart className={cn("w-6 h-6 transition-all",liked?"fill-primary text-primary scale-110":"text-muted-foreground")}/>
          </button>
        </div>
        {/* Progress */}
        <div className="mb-5">
          <TideSlider value={progress} onChange={onSeek} accent={song.gradient[0]}/>
          <div className="flex justify-between mt-2"><span className="text-xs text-muted-foreground font-mono">1:42</span><span className="text-xs text-muted-foreground font-mono">{song.duration}</span></div>
        </div>
        {/* Controls */}
        <div className="flex items-center justify-between mb-4">
          <button onClick={()=>setShuffle(!shuffle)} className={cn("w-11 h-11 rounded-full flex items-center justify-center hover:bg-muted/50",shuffle?"text-primary":"text-muted-foreground")}><Shuffle className="w-5 h-5"/></button>
          <button onClick={onPrev} className="w-12 h-12 rounded-full flex items-center justify-center text-foreground hover:bg-muted/50 active:scale-90"><SkipBack className="w-7 h-7 fill-foreground"/></button>
          <motion.button whileTap={{scale:0.9}} onClick={onPlayPause} className="w-16 h-16 rounded-full flex items-center justify-center text-white shadow-lg"
            style={{background:`linear-gradient(135deg,${song.gradient[0]},${song.gradient[1]})`,boxShadow:`0 8px 24px ${song.gradient[0]}60`}}>
            {isPlaying?<Pause className="w-7 h-7 fill-white"/>:<Play className="w-7 h-7 fill-white ml-1"/>}
          </motion.button>
          <button onClick={onNext} className="w-12 h-12 rounded-full flex items-center justify-center text-foreground hover:bg-muted/50 active:scale-90"><SkipForward className="w-7 h-7 fill-foreground"/></button>
          <button onClick={()=>setRepeat(!repeat)} className={cn("w-11 h-11 rounded-full flex items-center justify-center hover:bg-muted/50",repeat?"text-primary":"text-muted-foreground")}><Repeat className="w-5 h-5"/></button>
        </div>
        {/* Volume */}
        <div className="flex items-center gap-3 mb-5">
          <VolumeX className="w-4 h-4 text-muted-foreground shrink-0"/>
          <TideSlider value={volume} onChange={onVolume} accent={song.gradient[1]}/>
          <Volume2 className="w-4 h-4 text-muted-foreground shrink-0"/>
        </div>
        {/* Tabs */}
        <div className="flex-1 min-h-0 flex flex-col">
          <UnderlineTabs tabs={[{id:"lyrics",label:"Lyrics"},{id:"queue",label:"Queue"},{id:"eq",label:"EQ"}]} active={activeTab} onChange={id=>setActiveTab(id as typeof activeTab)}/>
          <div className="flex-1 overflow-y-auto pt-4 pb-8 hide-scrollbar">
            {activeTab==="lyrics"&&<div className="space-y-3">{LYRICS_LINES.map((l,i)=><p key={i} className={cn("text-sm leading-relaxed",l===""?"h-2":i===3?"text-foreground font-semibold text-base":"text-muted-foreground")}>{l}</p>)}</div>}
            {activeTab==="queue"&&<div className="space-y-1">{SONGS.map(s=>(
              <div key={s.id} className={cn("flex items-center gap-3 p-3 rounded-2xl",s.id===song.id?"bg-primary/10":"hover:bg-muted/50")}>
                <div className="w-10 h-10 rounded-xl shrink-0" style={{background:`linear-gradient(135deg,${s.gradient[0]},${s.gradient[1]})`}}/>
                <div className="flex-1 min-w-0"><p className={cn("text-sm font-medium truncate",s.id===song.id?"text-primary":"text-foreground")}>{s.title}</p><p className="text-xs text-muted-foreground">{s.artist}</p></div>
                <span className="text-xs text-muted-foreground font-mono">{s.duration}</span>
              </div>
            ))}</div>}
            {activeTab==="eq"&&<div className="space-y-4 pt-2">{["Sub Bass","Bass","Low Mid","Mid","High Mid","Presence","Brilliance"].map((b,i)=><TideSlider key={b} value={50+(i%2===0?12:-8)} onChange={()=>{}} label={b} accent={song.gradient[0]}/>)}</div>}
          </div>
        </div>
      </div>
    </motion.div>
  );
}

// ─────────────────────────────────────────────────────────────
// RIGHT PANEL (Desktop Lyrics / Queue)
// ─────────────────────────────────────────────────────────────
function RightPanelView({ panel, song, onClose }: { panel:RightPanel; song:Song|null; onClose:()=>void }) {
  return (
    <motion.aside initial={{width:0,opacity:0}} animate={{width:288,opacity:1}} exit={{width:0,opacity:0}} transition={{type:"spring",stiffness:350,damping:35}}
      className="shrink-0 border-l border-border bg-card/60 backdrop-blur-xl h-full flex flex-col overflow-hidden">
      <div className="flex items-center justify-between px-5 py-4 border-b border-border shrink-0">
        <h3 className="text-sm font-bold text-foreground">{panel==="lyrics"?"Lyrics":"Queue"}</h3>
        <button onClick={onClose} className="w-8 h-8 rounded-xl bg-muted flex items-center justify-center hover:bg-muted/80"><X className="w-4 h-4 text-muted-foreground"/></button>
      </div>
      {!song ? (
        <div className="flex-1 flex items-center justify-center p-6 text-center">
          <div><Music2 className="w-8 h-8 text-muted-foreground mx-auto mb-3"/><p className="text-sm text-muted-foreground">Nothing playing</p></div>
        </div>
      ) : panel==="lyrics" ? (
        <div className="flex-1 overflow-y-auto px-5 py-4 hide-scrollbar space-y-3">
          <div className="flex items-center gap-3 mb-5 p-3 rounded-2xl bg-muted/50">
            <div className="w-10 h-10 rounded-xl shrink-0" style={{background:`linear-gradient(135deg,${song.gradient[0]},${song.gradient[1]})`}}/>
            <div><p className="text-sm font-semibold text-foreground">{song.title}</p><p className="text-xs text-muted-foreground">{song.artist}</p></div>
          </div>
          {LYRICS_LINES.map((l,i)=>(
            <p key={i} className={cn("text-sm leading-loose",l===""?"h-3":i===3?"text-foreground font-semibold":"text-muted-foreground hover:text-foreground transition-colors cursor-pointer")}>{l}</p>
          ))}
        </div>
      ) : (
        <div className="flex-1 overflow-y-auto px-3 py-3 hide-scrollbar space-y-1">
          <p className="text-xs text-muted-foreground px-3 mb-3 font-medium">Up Next — {SONGS.length} songs</p>
          {SONGS.map(s=>(
            <div key={s.id} className={cn("flex items-center gap-3 p-3 rounded-2xl transition-colors cursor-pointer",s.id===song.id?"bg-primary/10":"hover:bg-muted/50")}>
              <div className="w-9 h-9 rounded-lg shrink-0 flex items-center justify-center" style={{background:`linear-gradient(135deg,${s.gradient[0]},${s.gradient[1]})`}}>
                {s.id===song.id?<div className="flex items-end gap-0.5 h-3">{[1,2,3].map(i=><motion.div key={i} className="w-0.5 bg-white rounded-full" animate={{height:["30%","100%","60%"]}} transition={{duration:0.7,repeat:Infinity,delay:i*0.15}}/>)}</div>:<Music2 className="w-3.5 h-3.5 text-white/80"/>}
              </div>
              <div className="flex-1 min-w-0"><p className={cn("text-xs font-semibold truncate",s.id===song.id?"text-primary":"text-foreground")}>{s.title}</p><p className="text-[10px] text-muted-foreground">{s.artist}</p></div>
              <span className="text-[10px] font-mono text-muted-foreground shrink-0">{s.duration}</span>
            </div>
          ))}
        </div>
      )}
    </motion.aside>
  );
}

// ─────────────────────────────────────────────────────────────
// APP PAGES
// ─────────────────────────────────────────────────────────────
function HeroBanner({ onPlay }: { onPlay:(s:Song)=>void }) {
  const [idx,setIdx] = useState(0);
  const items = PLAYLISTS.slice(0,4);
  useEffect(()=>{const t=setInterval(()=>setIdx(i=>(i+1)%items.length),5000);return()=>clearInterval(t);},[items.length]);
  const item = items[idx];
  return (
    <div className="relative rounded-[32px] overflow-hidden h-52 mb-6">
      <AnimatePresence mode="wait">
        <motion.div key={idx} initial={{opacity:0,scale:1.05}} animate={{opacity:1,scale:1}} exit={{opacity:0,scale:0.97}} transition={{duration:0.5}}
          className="absolute inset-0" style={{background:`linear-gradient(135deg,${item.gradient[0]},${item.gradient[1]})`}}/>
      </AnimatePresence>
      <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-transparent to-transparent"/>
      <div className="absolute inset-0 flex flex-col justify-end p-6">
        <span className="text-white/70 text-xs font-bold uppercase tracking-widest mb-1">Featured Playlist</span>
        <h2 className="text-2xl font-bold text-white mb-1">{item.title}</h2>
        <p className="text-sm text-white/70 mb-4">{item.description}</p>
        <div className="flex items-center gap-3">
          <motion.button whileTap={{scale:0.95}} onClick={()=>onPlay(SONGS[0])} className="flex items-center gap-2 px-5 py-2.5 bg-white text-gray-900 rounded-full text-sm font-bold hover:bg-white/90 transition-colors"><Play className="w-4 h-4 fill-gray-900"/>Play</motion.button>
          <button className="flex items-center gap-2 px-5 py-2.5 bg-white/20 text-white rounded-full text-sm font-semibold backdrop-blur-sm hover:bg-white/30 transition-colors"><Bookmark className="w-4 h-4"/>Save</button>
        </div>
      </div>
      <div className="absolute top-4 right-4 flex gap-1.5">
        {items.map((_,i)=><button key={i} onClick={()=>setIdx(i)} className={cn("rounded-full transition-all",i===idx?"w-5 h-1.5 bg-white":"w-1.5 h-1.5 bg-white/40")}/>)}
      </div>
    </div>
  );
}

function HomePage({ onPlay }: { onPlay:(s:Song)=>void }) {
  return (
    <div className="px-4 pt-2 pb-4">
      <HeroBanner onPlay={onPlay}/>
      <div className="mb-6"><SectionHeader title="Continue Listening" action="See All"/>
        <div className="flex gap-4 overflow-x-auto pb-2 hide-scrollbar">{ALBUMS.slice(0,6).map(a=><AlbumCard key={a.id} album={a} onClick={()=>onPlay(SONGS[a.id-1]||SONGS[0])}/>)}</div></div>
      <div className="mb-6"><SectionHeader title="Recently Added" action="See All"/>
        <div className="flex gap-4 overflow-x-auto pb-2 hide-scrollbar">{ALBUMS.slice(2,8).map(a=><AlbumCard key={a.id} album={a} size="sm" onClick={()=>onPlay(SONGS[a.id-1]||SONGS[0])}/>)}</div></div>
      <div className="mb-6"><SectionHeader title="Recommended Artists" action="See All"/>
        <div className="flex gap-4 overflow-x-auto pb-2 hide-scrollbar">{ARTISTS.map(a=><ArtistCard key={a.id} artist={a}/>)}</div></div>
      <div className="mb-6"><SectionHeader title="Pinned Playlists" action="See All"/>
        <div className="flex gap-4 overflow-x-auto pb-2 hide-scrollbar">{PLAYLISTS.map(p=><PlaylistCard key={p.id} playlist={p} onClick={()=>onPlay(SONGS[p.id-1]||SONGS[0])}/>)}</div></div>
      <div className="mb-6"><SectionHeader title="Recently Played"/>
        <div className="space-y-1">{SONGS.slice(0,6).map(s=><MusicCard key={s.id} song={s} onPlay={onPlay}/>)}</div></div>
    </div>
  );
}

function SearchPage({ onPlay }: { onPlay:(s:Song)=>void }) {
  const [q,setQ] = useState("");
  const cats = ["Electronic","Ambient","Synthwave","Techno","IDM","Post-Rock","Shoegaze","Experimental","Jazz","Classical"].map((n,i)=>({name:n,gradient:G[i%8]}));
  return (
    <div className="px-4 pt-2 pb-4">
      <div className="relative mb-6">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground"/>
        <input type="text" placeholder="Songs, artists, albums, folders, sources…" value={q} onChange={e=>setQ(e.target.value)}
          className="w-full h-12 pl-11 pr-4 bg-muted rounded-2xl text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30 transition-all"/>
      </div>
      {!q&&(<>
        <div className="mb-6"><SectionHeader title="Recent Searches"/>
          <div className="flex flex-wrap gap-2">{["Luna Waves","Synthwave","Midnight Cascade","Hi-Res","Ambient"].map(s=>(
            <button key={s} className="flex items-center gap-2 px-4 py-2 bg-muted rounded-full text-sm text-muted-foreground hover:text-foreground transition-colors"><Search className="w-3.5 h-3.5"/>{s}</button>
          ))}</div></div>
        <div className="mb-6"><SectionHeader title="Browse Genres"/>
          <div className="grid grid-cols-2 gap-3">{cats.map(c=>(
            <motion.button key={c.name} whileHover={{scale:1.02}} whileTap={{scale:0.97}} className="relative h-20 rounded-3xl overflow-hidden flex items-end p-4"
              style={{background:`linear-gradient(135deg,${c.gradient[0]},${c.gradient[1]})`}}>
              <span className="text-sm font-bold text-white">{c.name}</span>
            </motion.button>
          ))}</div></div>
        <div><SectionHeader title="Trending Now"/>
          <div className="space-y-1">{SONGS.map(s=><MusicCard key={s.id} song={s} onPlay={onPlay}/>)}</div></div>
      </>)}
      {q&&(
        <div><SectionHeader title={`"${q}"`}/>
          <div className="space-y-1">{SONGS.filter(s=>s.title.toLowerCase().includes(q.toLowerCase())||s.artist.toLowerCase().includes(q.toLowerCase())).map(s=><MusicCard key={s.id} song={s} onPlay={onPlay}/>)}</div>
          {SONGS.filter(s=>s.title.toLowerCase().includes(q.toLowerCase())||s.artist.toLowerCase().includes(q.toLowerCase())).length===0&&(
            <EmptyState icon={<Search className="w-7 h-7"/>} title="No results" subtitle={`Nothing found for "${q}"`}/>
          )}</div>
      )}
    </div>
  );
}

const LIB_TABS = [
  {id:"songs",label:"Songs"},{id:"albums",label:"Albums"},{id:"artists",label:"Artists"},
  {id:"genres",label:"Genres"},{id:"folders",label:"Folders"},{id:"playlists",label:"Playlists"},
  {id:"favorites",label:"Favorites"},{id:"downloads",label:"Downloads"},{id:"history",label:"History"},
  {id:"recently-added",label:"Recently Added"},{id:"recently-played",label:"Recently Played"},
  {id:"lossless",label:"Lossless"},{id:"hi-res",label:"Hi-Res"},{id:"sources",label:"Sources"},
];

function LibraryPage({ onPlay }: { onPlay:(s:Song)=>void }) {
  const [tab,setTab] = useState<LibTab>("songs");
  const sources = [
    {name:"Local Storage",type:"Local",icon:<HardDrive className="w-5 h-5"/>,status:"connected" as const,storage:"24.6 GB",tracks:1284,gradient:G[2]},
    {name:"Personal NAS",type:"WebDAV",icon:<Server className="w-5 h-5"/>,status:"connected" as const,storage:"128 GB",tracks:5820,gradient:G[1]},
    {name:"OneDrive Music",type:"OneDrive",icon:<Cloud className="w-5 h-5"/>,status:"syncing" as const,storage:"8.2 GB",tracks:342,gradient:G[0]},
    {name:"Jellyfin Home",type:"Jellyfin",icon:<Radio className="w-5 h-5"/>,status:"error" as const,storage:"512 GB",tracks:18200,gradient:G[3]},
    {name:"Plex Server",type:"Plex",icon:<Disc3 className="w-5 h-5"/>,status:"idle" as const,storage:"256 GB",tracks:8400,gradient:G[4]},
    {name:"Navidrome",type:"Navidrome",icon:<Music2 className="w-5 h-5"/>,status:"connected" as const,storage:"64 GB",tracks:3100,gradient:G[5]},
  ];
  const genres = ["Electronic","Ambient","Synthwave","Techno","IDM","Post-Rock","Shoegaze","Experimental","Jazz","Classical"];
  return (
    <div className="px-4 pt-2 pb-4">
      <div className="mb-4 overflow-x-auto hide-scrollbar"><PillTabs tabs={LIB_TABS} active={tab} onChange={id=>setTab(id as LibTab)}/></div>
      {tab==="songs"&&<div className="space-y-1"><div className="flex items-center justify-between mb-3"><span className="text-sm text-muted-foreground">{SONGS.length} songs · {SONGS.reduce((a,_s)=>a,0)??"~27 min"}</span><div className="flex gap-1"><Btn variant="ghost" size="sm" icon={<Filter className="w-4 h-4"/>} iconOnly/><Btn variant="ghost" size="sm" icon={<List className="w-4 h-4"/>} iconOnly/></div></div>{SONGS.map(s=><MusicCard key={s.id} song={s} onPlay={onPlay}/>)}</div>}
      {tab==="albums"&&<div className="grid grid-cols-2 sm:grid-cols-3 gap-4">{ALBUMS.map(a=><AlbumCard key={a.id} album={a} onClick={()=>onPlay(SONGS[a.id-1]||SONGS[0])}/>)}</div>}
      {tab==="artists"&&<div className="grid grid-cols-3 gap-4">{ARTISTS.map(a=><ArtistCard key={a.id} artist={a}/>)}</div>}
      {tab==="genres"&&<div className="grid grid-cols-2 gap-3">{genres.map((g,i)=>(
        <motion.div key={g} whileHover={{scale:1.02}} className="h-24 rounded-3xl flex items-end p-4 cursor-pointer overflow-hidden relative"
          style={{background:`linear-gradient(135deg,${G[i%8][0]},${G[i%8][1]})`}}>
          <span className="font-bold text-white text-sm">{g}</span>
          <div className="absolute top-3 right-3 bg-white/20 rounded-xl px-2 py-1"><span className="text-white text-xs font-semibold">{Math.floor(Math.random()*20+5)} albums</span></div>
        </motion.div>
      ))}</div>}
      {tab==="folders"&&<div className="space-y-1">{["/Music/Electronic","/Music/Ambient","/Downloads/Music","/Synced/WebDAV","/SD Card/Music"].map(f=>(
        <div key={f} className="flex items-center gap-3 p-3 rounded-2xl hover:bg-muted/60 cursor-pointer">
          <div className="w-10 h-10 rounded-xl bg-muted flex items-center justify-center"><Folder className="w-5 h-5 text-muted-foreground"/></div>
          <div className="flex-1 min-w-0"><p className="text-sm font-medium text-foreground">{f.split("/").pop()}</p><p className="text-xs text-muted-foreground">{f}</p></div>
          <ChevronRight className="w-4 h-4 text-muted-foreground"/>
        </div>
      ))}</div>}
      {tab==="playlists"&&<div className="grid grid-cols-2 gap-4">{PLAYLISTS.map(p=><PlaylistCard key={p.id} playlist={p} onClick={()=>onPlay(SONGS[p.id-1]||SONGS[0])}/>)}</div>}
      {tab==="favorites"&&<div className="space-y-1">{SONGS.filter(s=>s.liked).map(s=><MusicCard key={s.id} song={s} onPlay={onPlay}/>)}</div>}
      {tab==="downloads"&&<EmptyState icon={<Download className="w-7 h-7"/>} title="No downloads yet" subtitle="Downloaded songs will appear here" action="Browse Library"/>}
      {tab==="history"&&<div className="space-y-1">{[...SONGS].reverse().map(s=><MusicCard key={s.id} song={s} onPlay={onPlay}/>)}</div>}
      {(tab==="recently-added"||tab==="recently-played")&&<div className="space-y-1">{SONGS.slice(0,6).map(s=><MusicCard key={s.id} song={s} onPlay={onPlay}/>)}</div>}
      {tab==="lossless"&&<div className="space-y-1"><div className="flex items-center gap-2 mb-3 px-1"><div className="px-2.5 py-1 rounded-lg text-xs font-bold" style={{background:"#3DCA8A20",color:"#3DCA8A",border:"1px solid #3DCA8A40"}}>Lossless</div><span className="text-sm text-muted-foreground">2 songs</span></div>{SONGS.filter(s=>s.quality==="lossless").map(s=><MusicCard key={s.id} song={s} onPlay={onPlay}/>)}</div>}
      {tab==="hi-res"&&<div className="space-y-1"><div className="flex items-center gap-2 mb-3 px-1"><div className="px-2.5 py-1 rounded-lg text-xs font-bold" style={{background:"#FFD93D20",color:"#FFD93D",border:"1px solid #FFD93D40"}}>Hi-Res</div><span className="text-sm text-muted-foreground">2 songs</span></div>{SONGS.filter(s=>s.quality==="hi-res").map(s=><MusicCard key={s.id} song={s} onPlay={onPlay}/>)}</div>}
      {tab==="sources"&&<div className="space-y-4"><div className="grid grid-cols-1 gap-4">{sources.map(s=><SourceCard key={s.name} source={s}/>)}</div><Btn variant="tonal" icon={<Plus className="w-4 h-4"/>} className="w-full rounded-2xl">Add Source</Btn></div>}
    </div>
  );
}

function SettingsPage() {
  const [mobileData,setMobileData]=useState(false);
  const [autoPlay,setAutoPlay]=useState(true);
  const [loudnorm,setLoudnorm]=useState(true);
  const [floatLyrics,setFloatLyrics]=useState(false);
  const [dynColor,setDynColor]=useState(true);
  const [blur,setBlur]=useState(true);
  const [autoLib,setAutoLib]=useState(true);
  const [plugins,setPlugins]=useState(true);
  return (
    <div className="px-4 pt-2 pb-6">
      <SettingsCard title="Transfer & Download">
        <SettingItem label="Allow Mobile Network" subtitle="Stream over cellular data" leading={<Wifi className="w-4 h-4"/>} trailing={<TideSwitch checked={mobileData} onChange={setMobileData}/>}/>
        <SettingItem label="Streaming Quality" subtitle="Hi-Res FLAC 24bit/192kHz" leading={<Gauge className="w-4 h-4"/>}/>
        <SettingItem label="Auto Download" subtitle="Liked songs" leading={<Download className="w-4 h-4"/>}/>
        <div className="px-4 py-4 border-t border-border/60">
          <p className="text-xs text-muted-foreground font-medium mb-3">Cache Size</p>
          <TideSlider value={60} onChange={()=>{}} accent="var(--tide-blue)"/>
          <div className="flex justify-between mt-1.5"><span className="text-xs text-muted-foreground font-mono">512 MB</span><span className="text-xs text-muted-foreground font-mono">4 GB</span></div>
        </div>
      </SettingsCard>

      <SettingsCard title="Playback">
        <SettingItem label="Auto Play" subtitle="Continue with similar songs" leading={<Play className="w-4 h-4"/>} trailing={<TideSwitch checked={autoPlay} onChange={setAutoPlay}/>}/>
        <SettingItem label="Repeat Mode" subtitle="Off" leading={<Repeat className="w-4 h-4"/>}/>
        <SettingItem label="Sleep Timer" subtitle="Off" leading={<Activity className="w-4 h-4"/>}/>
        <SettingItem label="Replay Gain" subtitle="Track gain" leading={<BarChart2 className="w-4 h-4"/>}/>
        <SettingItem label="Loudness Normalization" leading={<Volume2 className="w-4 h-4"/>} trailing={<TideSwitch checked={loudnorm} onChange={setLoudnorm}/>}/>
        <SettingItem label="Equalizer" subtitle="Default" leading={<SlidersHorizontal className="w-4 h-4"/>}/>
      </SettingsCard>

      <SettingsCard title="Library">
        <SettingItem label="Source Manager" subtitle="6 sources connected" leading={<Database className="w-4 h-4"/>}/>
        <SettingItem label="Library Manager" subtitle="7,446 songs indexed" leading={<Library className="w-4 h-4"/>}/>
        <SettingItem label="Auto Scan" subtitle="Scan library on startup" leading={<RefreshCw className="w-4 h-4"/>} trailing={<TideSwitch checked={autoLib} onChange={setAutoLib}/>}/>
        <SettingItem label="Metadata" subtitle="Fetch artwork & tags automatically" leading={<FileText className="w-4 h-4"/>}/>
      </SettingsCard>

      <SettingsCard title="Lyrics">
        <SettingItem label="Floating Lyrics" subtitle="Show on lock screen" leading={<Mic className="w-4 h-4"/>} trailing={<TideSwitch checked={floatLyrics} onChange={setFloatLyrics}/>}/>
        <SettingItem label="Font Size" subtitle="Medium" leading={<Hash className="w-4 h-4"/>}/>
        <SettingItem label="Translation" subtitle="English" leading={<Globe className="w-4 h-4"/>}/>
      </SettingsCard>

      <SettingsCard title="Appearance">
        <SettingItem label="Dynamic Color" subtitle="Adapt UI to artwork" leading={<Palette className="w-4 h-4"/>} trailing={<TideSwitch checked={dynColor} onChange={setDynColor}/>}/>
        <SettingItem label="Accent Color" subtitle="TidePink #FF5B8A" leading={<div className="w-4 h-4 rounded-full" style={{background:"var(--tide-pink)"}}/>}/>
        <SettingItem label="Blur & Material" subtitle="Glassmorphism effects" leading={<Layers className="w-4 h-4"/>} trailing={<TideSwitch checked={blur} onChange={setBlur}/>}/>
        <SettingItem label="Icon Shape" subtitle="Rounded" leading={<Package className="w-4 h-4"/>}/>
        <SettingItem label="Theme" subtitle="System" leading={<Sun className="w-4 h-4"/>}/>
      </SettingsCard>

      <SettingsCard title="Plugins">
        <SettingItem label="Enable Plugins" subtitle="Third-party extensions" leading={<Puzzle className="w-4 h-4"/>} trailing={<TideSwitch checked={plugins} onChange={setPlugins}/>}/>
        <SettingItem label="Plugin Manager" subtitle="0 plugins installed" leading={<Package className="w-4 h-4"/>}/>
        <SettingItem label="Explore Plugins" leading={<Sparkles className="w-4 h-4"/>}/>
      </SettingsCard>

      <SettingsCard title="Advanced Sources">
        {[
          {name:"WebDAV",icon:<Server className="w-4 h-4"/>},{name:"OneDrive",icon:<Cloud className="w-4 h-4"/>},
          {name:"Google Drive",icon:<Database className="w-4 h-4"/>},{name:"SMB / NAS",icon:<HardDrive className="w-4 h-4"/>},
          {name:"Emby",icon:<Radio className="w-4 h-4"/>},{name:"Plex",icon:<Disc3 className="w-4 h-4"/>},
          {name:"Jellyfin",icon:<Music2 className="w-4 h-4"/>},{name:"Navidrome",icon:<Music2 className="w-4 h-4"/>},
          {name:"Dropbox",icon:<Cloud className="w-4 h-4"/>},{name:"Custom API",icon:<Code2 className="w-4 h-4"/>},
        ].map(s=><SettingItem key={s.name} label={s.name} leading={s.icon}/>)}
      </SettingsCard>

      <SettingsCard title="About">
        <SettingItem label="TideTunes" subtitle="Version 3.0.0 · Build 2024.12" leading={<Music2 className="w-4 h-4"/>} trailing={null}/>
        <SettingItem label="Design System" subtitle="v3.0 · HyperOS × Apple Music" leading={<Sparkles className="w-4 h-4"/>} trailing={null}/>
        <SettingItem label="Open Source Licenses" leading={<FileText className="w-4 h-4"/>}/>
      </SettingsCard>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// DESIGN SYSTEM PAGES
// ─────────────────────────────────────────────────────────────
function DSCover() {
  const structure = [
    {n:"00 Cover",sub:["Brand","Vision","Principles","Philosophy"]},
    {n:"01 Foundation",sub:["Color","Typography","Grid","Elevation","Blur","Radius","Motion","Icons"]},
    {n:"02 Tokens",sub:["Color Tokens","Space Tokens","Radius Tokens","Motion Tokens","Shadow Tokens"]},
    {n:"03 Components",sub:["Buttons","Navigation","Cards","Player","Settings","Search","Dialogs","Feedback"]},
    {n:"04 Adaptive Layout",sub:["Phone","Fold","Tablet","Auto","iPhone","iPad","Desktop"]},
    {n:"05 Pages",sub:["Home","Search","Library","Settings","Player","Source Manager"]},
    {n:"06 Prototype",sub:[]},
    {n:"07 Motion",sub:["Spring","Shared Element","Blur Morph","Hero"]},
    {n:"08 Dev Mode",sub:[]},
    {n:"09 Compose",sub:["MiuixScaffold","CardGroup","SuperArrow","Preference"]},
  ];
  const principles = ["Simple","Calm","Immersive","Music First","Content First","Adaptive","Native","Cross Platform","Plugin Driven"];
  return (
    <div className="px-4 pt-2 pb-8 space-y-10">
      {/* Hero */}
      <div className="relative rounded-[32px] overflow-hidden p-8 pb-10" style={{background:`linear-gradient(135deg,${G[0][0]}30,${G[1][1]}20,transparent)`}}>
        <div className="absolute inset-0 rounded-[32px] border border-primary/20"/>
        <div className="relative z-10">
          <div className="flex items-center gap-3 mb-6">
            <div className="w-14 h-14 rounded-[18px] flex items-center justify-center shadow-xl" style={{background:"linear-gradient(135deg,var(--tide-pink),var(--tide-purple))"}}>
              <Music2 className="w-7 h-7 text-white"/>
            </div>
            <div>
              <h1 className="text-3xl font-black tracking-tight" style={{background:`linear-gradient(135deg,${G[0][0]},${G[1][1]})`,WebkitBackgroundClip:"text",WebkitTextFillColor:"transparent",backgroundClip:"text"}}>TideTunes DS</h1>
              <p className="text-sm text-muted-foreground font-medium">Design System v3.0</p>
            </div>
          </div>
          <p className="text-base text-foreground font-medium mb-1">One Library. Every Source.</p>
          <p className="text-sm text-muted-foreground max-w-md">Apple Music Information Architecture × HyperOS Design Language × Compose Multiplatform. A production-ready cross-platform design system for Android, iOS, and Desktop.</p>
        </div>
      </div>

      {/* Principles */}
      <section>
        <SectionHeader title="Design Principles"/>
        <div className="flex flex-wrap gap-2.5">
          {principles.map((p,i)=>(
            <div key={p} className="flex items-center gap-2.5 px-4 py-2.5 rounded-full border border-border bg-card text-sm font-semibold text-foreground hover:border-primary/40 transition-colors cursor-default">
              <div className="w-2 h-2 rounded-full" style={{background:G[i%8][0]}}/>
              {p}
            </div>
          ))}
        </div>
      </section>

      {/* Structure */}
      <section>
        <SectionHeader title="File Structure"/>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {structure.map((s,i)=>(
            <div key={s.n} className="bg-card rounded-2xl border border-border p-4">
              <div className="flex items-center gap-2 mb-2">
                <span className="text-xs font-mono font-bold text-muted-foreground">{String(i).padStart(2,"0")}</span>
                <p className="text-sm font-bold text-foreground">{s.n.split(" ").slice(1).join(" ")}</p>
              </div>
              {s.sub.length>0&&<div className="flex flex-wrap gap-1.5">{s.sub.map(sub=>(
                <span key={sub} className="text-[10px] text-muted-foreground bg-muted px-2 py-1 rounded-lg font-medium">{sub}</span>
              ))}</div>}
            </div>
          ))}
        </div>
      </section>

      {/* Platform targets */}
      <section>
        <SectionHeader title="Platform Targets"/>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[{i:<Smartphone className="w-5 h-5"/>,n:"Android Phone",b:"Compact · Medium"},{i:<Tablet className="w-5 h-5"/>,n:"Android Tablet",b:"Expanded · Large"},{i:<Monitor className="w-5 h-5"/>,n:"Desktop",b:"Large · XL"},{i:<Smartphone className="w-5 h-5"/>,n:"Automotive",b:"Landscape Only"}].map(p=>(
            <div key={p.n} className="bg-card rounded-2xl border border-border p-4 flex flex-col items-center text-center gap-2">
              <div className="w-10 h-10 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">{p.i}</div>
              <p className="text-sm font-semibold text-foreground">{p.n}</p>
              <p className="text-xs text-muted-foreground">{p.b}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function DSFoundation() {
  const colors = [
    {name:"TidePink",hex:"#FF5B8A",role:"Primary"},{name:"TidePurple",hex:"#7A6CFF",role:"Secondary"},
    {name:"TideOrange",hex:"#FF8A3D",role:"Support"},{name:"TideGreen",hex:"#3DCA8A",role:"Support"},
    {name:"TideBlue",hex:"#3D9AFF",role:"Support"},{name:"TideYellow",hex:"#FFD93D",role:"Support"},
  ];
  const typescale = [
    {name:"Display",cls:"text-4xl font-black",size:"36px · 900"},
    {name:"Headline",cls:"text-3xl font-bold",size:"30px · 700"},
    {name:"Title Large",cls:"text-2xl font-bold",size:"24px · 700"},
    {name:"Title",cls:"text-xl font-semibold",size:"20px · 600"},
    {name:"Body Large",cls:"text-base",size:"16px · 400"},
    {name:"Body",cls:"text-sm",size:"14px · 400"},
    {name:"Label",cls:"text-xs font-medium tracking-wide",size:"12px · 500"},
    {name:"Caption",cls:"text-[10px] font-bold tracking-widest uppercase",size:"10px · 700"},
  ];
  const breakpoints = [
    {name:"Compact",range:"0 – 599dp",nav:"Bottom Navigation",layout:"Single Pane",color:G[0][0]},
    {name:"Medium",range:"600 – 839dp",nav:"Bottom Navigation",layout:"Single / Two Pane",color:G[1][0]},
    {name:"Expanded",range:"840 – 1279dp",nav:"Navigation Rail",layout:"Two Pane",color:G[2][0]},
    {name:"Large",range:"1280+ dp",nav:"Sidebar",layout:"Three Pane",color:G[3][0]},
    {name:"XL",range:"1600+ dp",nav:"Sidebar (Wide)",layout:"Three Pane+",color:G[4][0]},
  ];
  const radii = [{l:"Small",v:12},{l:"Medium",v:20},{l:"Large",v:28},{l:"XL",v:36},{l:"Full",v:9999}];
  const elevations = [
    {n:"Surface",s:"none"},{n:"Card",s:"0 2px 8px rgba(0,0,0,0.08)"},{n:"Popup",s:"0 8px 24px rgba(0,0,0,0.14)"},
    {n:"Floating",s:"0 12px 40px rgba(0,0,0,0.22)"},{n:"Overlay",s:"0 24px 64px rgba(0,0,0,0.35)"},
  ];
  return (
    <div className="space-y-10 px-4 py-2 pb-8">
      <section>
        <SectionHeader title="Brand Colors"/>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {colors.map(c=>(
            <div key={c.name} className="bg-card rounded-3xl border border-border overflow-hidden">
              <div className="h-16" style={{background:c.hex}}/>
              <div className="p-3"><p className="text-sm font-semibold text-foreground">{c.name}</p>
                <p className="text-xs font-mono text-muted-foreground">{c.hex}</p>
                <span className="text-[10px] font-bold uppercase tracking-widest" style={{color:c.hex}}>{c.role}</span>
              </div>
            </div>
          ))}
        </div>
      </section>
      <section>
        <SectionHeader title="Gradient Pairs"/>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {G.map((g,i)=>(
            <div key={i} className="rounded-2xl overflow-hidden">
              <div className="h-20 rounded-2xl" style={{background:`linear-gradient(135deg,${g[0]},${g[1]})`}}/>
              <p className="text-[9px] font-mono text-muted-foreground mt-1.5 text-center">{g[0]}</p>
            </div>
          ))}
        </div>
      </section>
      <section>
        <SectionHeader title="Typography Scale · Plus Jakarta Sans"/>
        <div className="bg-card rounded-3xl border border-border overflow-hidden divide-y divide-border/60">
          {typescale.map(t=>(
            <div key={t.name} className="flex items-baseline justify-between px-5 py-4 gap-4">
              <p className={cn("text-foreground truncate",t.cls)}>TideTunes</p>
              <div className="text-right shrink-0"><p className="text-xs font-semibold text-foreground">{t.name}</p><p className="text-[10px] font-mono text-muted-foreground">{t.size}</p></div>
            </div>
          ))}
        </div>
      </section>
      <section>
        <SectionHeader title="Responsive Breakpoints"/>
        <div className="space-y-2">
          {breakpoints.map(bp=>(
            <div key={bp.name} className="flex items-center gap-4 p-4 bg-card rounded-2xl border border-border">
              <div className="w-3 h-3 rounded-full shrink-0" style={{background:bp.color}}/>
              <div className="w-24 shrink-0"><p className="text-sm font-bold text-foreground">{bp.name}</p><p className="text-[10px] font-mono text-muted-foreground">{bp.range}</p></div>
              <div className="flex gap-2 flex-wrap">
                <span className="text-xs bg-muted text-muted-foreground px-2.5 py-1 rounded-xl font-medium">{bp.nav}</span>
                <span className="text-xs bg-muted text-muted-foreground px-2.5 py-1 rounded-xl font-medium">{bp.layout}</span>
              </div>
            </div>
          ))}
        </div>
      </section>
      <section>
        <SectionHeader title="Corner Radius"/>
        <div className="flex flex-wrap gap-6 items-end">
          {radii.map(r=>(
            <div key={r.l} className="flex flex-col items-center gap-2">
              <div className="w-20 h-20 bg-primary/15 border-2 border-primary/30" style={{borderRadius:r.v===9999?9999:r.v}}/>
              <p className="text-xs font-semibold text-foreground">{r.l}</p>
              <p className="text-[10px] font-mono text-muted-foreground">{r.v===9999?"∞":r.v+"px"}</p>
            </div>
          ))}
        </div>
      </section>
      <section>
        <SectionHeader title="Elevation Scale"/>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
          {elevations.map(e=>(
            <div key={e.n} className="bg-card rounded-2xl p-4" style={{boxShadow:e.s}}>
              <p className="text-sm font-semibold text-foreground">{e.n}</p>
              <p className="text-[10px] font-mono text-muted-foreground mt-1 break-all">{e.s||"none"}</p>
            </div>
          ))}
        </div>
      </section>
      <section>
        <SectionHeader title="Spacing Scale · 8dp Grid"/>
        <div className="flex flex-wrap gap-4 items-end">
          {[4,8,12,16,20,24,32,40,48].map(s=>(
            <div key={s} className="flex flex-col items-center gap-2">
              <div className="bg-secondary/30 rounded" style={{width:s,height:s}}/>
              <span className="text-[10px] font-mono text-muted-foreground">{s}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function DSTokens() {
  const tokenGroups = [
    { title:"Color Tokens", items:[
      {name:"--tide-pink",value:"#FF5B8A",type:"color",swatch:true},{name:"--tide-purple",value:"#7A6CFF",type:"color",swatch:true},
      {name:"--background",value:"#0C0A14 / #F4F2FA",type:"color"},{name:"--card",value:"#161224 / #FFFFFF",type:"color"},
      {name:"--primary",value:"#FF5B8A",type:"color",swatch:true},{name:"--muted-foreground",value:"#9B97B0 / #6B6880",type:"color"},
    ]},
    { title:"Spacing Tokens", items:[
      {name:"--space-1",value:"4dp"},{name:"--space-2",value:"8dp"},{name:"--space-3",value:"12dp"},
      {name:"--space-4",value:"16dp"},{name:"--space-5",value:"20dp"},{name:"--space-6",value:"24dp"},
      {name:"--space-8",value:"32dp"},{name:"--space-10",value:"40dp"},{name:"--space-12",value:"48dp"},
    ]},
    { title:"Radius Tokens", items:[
      {name:"--radius-xs",value:"8px"},{name:"--radius-sm",value:"12px"},{name:"--radius-md",value:"20px"},
      {name:"--radius-lg",value:"28px"},{name:"--radius-xl",value:"36px"},{name:"--radius-full",value:"9999px"},
    ]},
    { title:"Elevation Tokens", items:[
      {name:"--shadow-surface",value:"none"},{name:"--shadow-card",value:"0 2px 8px rgba(0,0,0,.08)"},
      {name:"--shadow-popup",value:"0 8px 24px rgba(0,0,0,.14)"},{name:"--shadow-floating",value:"0 12px 40px rgba(0,0,0,.22)"},
      {name:"--shadow-overlay",value:"0 24px 64px rgba(0,0,0,.35)"},
    ]},
    { title:"Blur Tokens", items:[
      {name:"--blur-none",value:"0px"},{name:"--blur-light",value:"8px"},
      {name:"--blur-medium",value:"20px"},{name:"--blur-heavy",value:"40px"},
    ]},
    { title:"Motion Tokens", items:[
      {name:"--spring-stiff",value:"stiffness: 400, damping: 30"},{name:"--spring-soft",value:"stiffness: 200, damping: 25"},
      {name:"--duration-fast",value:"150ms"},{name:"--duration-normal",value:"300ms"},{name:"--duration-slow",value:"500ms"},
    ]},
  ];
  return (
    <div className="space-y-6 px-4 py-2 pb-8">
      <div className="bg-card/60 rounded-3xl border border-border p-5 mb-2">
        <p className="text-sm font-semibold text-foreground mb-1">Design Tokens</p>
        <p className="text-xs text-muted-foreground">All tokens are CSS custom properties mapped to Tailwind utilities via <code className="font-mono text-primary bg-primary/10 px-1.5 py-0.5 rounded">@theme inline</code>. Every token has a light-mode and dark-mode value.</p>
      </div>
      {tokenGroups.map(g=>(
        <section key={g.title}>
          <SectionHeader title={g.title}/>
          <div className="bg-card rounded-3xl border border-border overflow-hidden divide-y divide-border/60">
            {g.items.map(item=>(
              <div key={item.name} className="flex items-center gap-4 px-4 py-3">
                {"swatch" in item && item.swatch && (
                  <div className="w-8 h-8 rounded-xl shrink-0 border border-border" style={{background:item.value as string}}/>
                )}
                <code className="text-xs font-mono text-primary flex-shrink-0">{item.name}</code>
                <code className="text-xs font-mono text-muted-foreground flex-1 truncate">{item.value as string}</code>
              </div>
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}

function DSComponents() {
  const [sw1,setSw1]=useState(true); const [sw2,setSw2]=useState(false);
  const [sl1,setSl1]=useState(65); const [sl2,setSl2]=useState(40);
  const [tab1,setTab1]=useState("a"); const [tab2,setTab2]=useState("b"); const [tab3,setTab3]=useState("c");
  return (
    <div className="space-y-10 px-4 py-2 pb-8">
      <section><SectionHeader title="Buttons"/>
        <div className="bg-card rounded-3xl border border-border p-5 space-y-4">
          <div className="flex flex-wrap gap-3"><Btn variant="filled">Filled</Btn><Btn variant="secondary">Secondary</Btn><Btn variant="tonal">Tonal</Btn><Btn variant="outlined">Outlined</Btn><Btn variant="ghost">Ghost</Btn></div>
          <div className="flex flex-wrap gap-3"><Btn variant="filled" size="sm">Small</Btn><Btn variant="filled" size="md">Medium</Btn><Btn variant="filled" size="lg">Large</Btn></div>
          <div className="flex flex-wrap gap-3 items-center">
            <Btn variant="filled" icon={<Play className="w-4 h-4 fill-white"/>}>Play</Btn>
            <Btn variant="tonal" icon={<Download className="w-4 h-4"/>}>Download</Btn>
            <Btn variant="outlined" icon={<Share2 className="w-4 h-4"/>}>Share</Btn>
            <Btn variant="filled" icon={<Play className="w-4 h-4 fill-white"/>} iconOnly/>
            <Btn variant="tonal" icon={<Heart className="w-4 h-4"/>} iconOnly/>
            <Btn variant="ghost" icon={<MoreHorizontal className="w-4 h-4"/>} iconOnly/>
          </div>
        </div>
      </section>
      <section><SectionHeader title="Quality Badges"/>
        <div className="bg-card rounded-3xl border border-border p-5 flex flex-wrap gap-3">
          <QualityBadge quality="lossless"/><QualityBadge quality="hi-res"/><QualityBadge quality="dolby"/>
        </div>
      </section>
      <section><SectionHeader title="Controls — Switch & Slider"/>
        <div className="bg-card rounded-3xl border border-border p-5 space-y-5">
          <div className="flex flex-wrap gap-8"><TideSwitch checked={sw1} onChange={setSw1} label="Dynamic Color"/><TideSwitch checked={sw2} onChange={setSw2} label="Blur Effect"/></div>
          <TideSlider value={sl1} onChange={setSl1} label="Volume"/>
          <TideSlider value={sl2} onChange={setSl2} label="Treble" accent="var(--tide-purple)"/>
        </div>
      </section>
      <section><SectionHeader title="Tabs"/>
        <div className="bg-card rounded-3xl border border-border p-5 space-y-5">
          <UnderlineTabs tabs={[{id:"a",label:"Songs"},{id:"b",label:"Albums"},{id:"c",label:"Artists"}]} active={tab1} onChange={setTab1}/>
          <PillTabs tabs={[{id:"a",label:"Albums"},{id:"b",label:"Playlists"},{id:"c",label:"Folders"},{id:"d",label:"Sources"}]} active={tab2} onChange={setTab2}/>
          <SegTabs tabs={[{id:"a",label:"Lyrics"},{id:"b",label:"Queue"},{id:"c",label:"EQ"}]} active={tab3} onChange={setTab3}/>
        </div>
      </section>
      <section><SectionHeader title="Album Cards"/>
        <div className="bg-card rounded-3xl border border-border p-5 overflow-x-auto hide-scrollbar">
          <div className="flex gap-4 pb-2">{ALBUMS.slice(0,4).map(a=><AlbumCard key={a.id} album={a}/>)}</div>
        </div>
      </section>
      <section><SectionHeader title="Artist Cards"/>
        <div className="bg-card rounded-3xl border border-border p-5 overflow-x-auto hide-scrollbar">
          <div className="flex gap-4 pb-2">{ARTISTS.slice(0,4).map(a=><ArtistCard key={a.id} artist={a}/>)}</div>
        </div>
      </section>
      <section><SectionHeader title="Playlist Cards"/>
        <div className="bg-card rounded-3xl border border-border p-5 overflow-x-auto hide-scrollbar">
          <div className="flex gap-4 pb-2">{PLAYLISTS.slice(0,4).map(p=><PlaylistCard key={p.id} playlist={p}/>)}</div>
        </div>
      </section>
      <section><SectionHeader title="Music List Items"/>
        <div className="bg-card rounded-3xl border border-border overflow-hidden">
          {SONGS.slice(0,4).map(s=><MusicCard key={s.id} song={s} onPlay={()=>{}} isPlaying={s.id===1}/>)}
        </div>
      </section>
      <section><SectionHeader title="Source Cards"/>
        <div className="space-y-4">
          <SourceCard source={{name:"Personal NAS",type:"WebDAV",icon:<Server className="w-5 h-5"/>,status:"connected",storage:"128 GB",tracks:5820,gradient:G[1]}}/>
          <SourceCard source={{name:"Jellyfin Home",type:"Jellyfin",icon:<Radio className="w-5 h-5"/>,status:"syncing",storage:"512 GB",tracks:18200,gradient:G[3]}}/>
        </div>
      </section>
      <section><SectionHeader title="Settings Items"/>
        <div className="bg-card rounded-3xl border border-border overflow-hidden divide-y divide-border/60">
          <SettingItem label="Streaming Quality" subtitle="Hi-Res FLAC" leading={<Gauge className="w-4 h-4"/>}/>
          <SettingItem label="Dynamic Color" leading={<Palette className="w-4 h-4"/>} trailing={<TideSwitch checked={true} onChange={()=>{}}/>}/>
          <SettingItem label="Delete Library" leading={<X className="w-4 h-4"/>} danger/>
        </div>
      </section>
      <section><SectionHeader title="Skeleton"/>
        <div className="bg-card rounded-3xl border border-border p-5 space-y-3">
          <div className="flex items-center gap-3"><SkeletonBlock className="w-11 h-11 rounded-xl"/><div className="flex-1 space-y-2"><SkeletonBlock className="h-4 w-3/4 rounded-xl"/><SkeletonBlock className="h-3 w-1/2 rounded-xl"/></div></div>
          <div className="flex gap-3">{[0,1,2,3].map(i=><div key={i} className="flex flex-col gap-2"><SkeletonBlock className="w-[140px] h-[140px] rounded-3xl"/><SkeletonBlock className="h-3 w-24 rounded-xl"/><SkeletonBlock className="h-2.5 w-16 rounded-xl"/></div>)}</div>
        </div>
      </section>
      <section><SectionHeader title="Empty State"/>
        <div className="bg-card rounded-3xl border border-border">
          <EmptyState icon={<Music2 className="w-7 h-7"/>} title="No songs found" subtitle="Add a source to get started" action="Add Source"/>
        </div>
      </section>
    </div>
  );
}

function DSPatterns() {
  return (
    <div className="space-y-10 px-4 py-2 pb-8">
      <section><SectionHeader title="Mini Player"/>
        <div className="bg-muted rounded-3xl p-4">
          <div className="relative flex items-center gap-3 px-4 h-[68px] rounded-[28px]" style={{background:"var(--card)",border:"1px solid var(--border)",boxShadow:"0 8px 32px rgba(0,0,0,0.15)"}}>
            <div className="absolute inset-0 rounded-[28px] overflow-hidden opacity-[0.08]" style={{background:`linear-gradient(90deg,${G[0][0]},${G[0][1]})`}}/>
            <div className="w-11 h-11 rounded-xl shrink-0" style={{background:`linear-gradient(135deg,${G[0][0]},${G[0][1]})`}}/>
            <div className="flex-1"><p className="text-sm font-semibold text-foreground">Midnight Cascade</p><p className="text-xs text-muted-foreground">Luna Waves</p></div>
            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-border rounded-full overflow-hidden"><div className="h-full w-[40%] rounded-full" style={{background:`linear-gradient(90deg,${G[0][0]},${G[0][1]})`}}/></div>
            <div className="flex items-center gap-1">
              <div className="w-10 h-10 rounded-full bg-muted/50 flex items-center justify-center"><Play className="w-5 h-5 fill-foreground"/></div>
              <div className="w-10 h-10 rounded-full bg-muted/50 flex items-center justify-center"><SkipForward className="w-5 h-5 text-foreground"/></div>
            </div>
          </div>
        </div>
      </section>
      <section><SectionHeader title="Navigation Bar (Phone · Compact)"/>
        <div className="bg-muted rounded-3xl p-4">
          <div className="flex items-center justify-around px-4 h-16 rounded-3xl" style={{background:"var(--card)",border:"1px solid var(--border)"}}>
            {[{i:<Home className="w-5 h-5"/>,l:"Home",a:true},{i:<Search className="w-5 h-5"/>,l:"Search",a:false},{i:<Library className="w-5 h-5"/>,l:"Library",a:false},{i:<Settings className="w-5 h-5"/>,l:"Settings",a:false}].map(item=>(
              <div key={item.l} className={cn("flex flex-col items-center gap-1 px-4 py-1 rounded-2xl",item.a?"text-primary":"text-muted-foreground")}>
                <div className={cn("p-1.5 rounded-xl",item.a?"bg-primary/15":"")}>{item.i}</div>
                <span className="text-[10px] font-semibold">{item.l}</span>
              </div>
            ))}
          </div>
        </div>
      </section>
      <section><SectionHeader title="Navigation Rail (Tablet · Expanded)"/>
        <div className="bg-muted rounded-3xl p-4">
          <div className="w-20 flex flex-col items-center py-4 gap-1 rounded-3xl" style={{background:"var(--card)",border:"1px solid var(--border)"}}>
            <div className="w-10 h-10 rounded-2xl mb-3 flex items-center justify-center" style={{background:"linear-gradient(135deg,var(--tide-pink),var(--tide-purple))"}}><Music2 className="w-5 h-5 text-white"/></div>
            {[{i:<Home className="w-4 h-4"/>,l:"Home",a:true},{i:<Search className="w-4 h-4"/>,l:"Search",a:false},{i:<Library className="w-4 h-4"/>,l:"Library",a:false},{i:<Settings className="w-4 h-4"/>,l:"Settings",a:false}].map(item=>(
              <div key={item.l} className={cn("flex flex-col items-center gap-1 w-full px-2 py-2.5 rounded-2xl",item.a?"bg-primary/15 text-primary":"text-muted-foreground")}>
                {item.i}<span className="text-[9px] font-bold">{item.l}</span>
              </div>
            ))}
          </div>
        </div>
      </section>
      <section><SectionHeader title="Desktop Layout (Large · XL)"/>
        <div className="bg-muted rounded-3xl p-4 overflow-hidden">
          <div className="bg-card rounded-3xl border border-border overflow-hidden" style={{minHeight:300}}>
            {/* Toolbar */}
            <div className="flex items-center gap-3 px-4 h-12 border-b border-border bg-card/80">
              <div className="flex gap-1.5"><div className="w-3 h-3 rounded-full bg-red-400"/><div className="w-3 h-3 rounded-full bg-yellow-400"/><div className="w-3 h-3 rounded-full bg-green-400"/></div>
              <div className="flex gap-1"><div className="w-6 h-6 rounded-lg bg-muted flex items-center justify-center"><ArrowLeft className="w-3 h-3 text-muted-foreground"/></div><div className="w-6 h-6 rounded-lg bg-muted flex items-center justify-center"><ArrowRight className="w-3 h-3 text-muted-foreground"/></div></div>
              <div className="flex-1 h-7 bg-muted rounded-xl flex items-center px-3"><Search className="w-3 h-3 text-muted-foreground mr-2"/><span className="text-xs text-muted-foreground">Search TideTunes…</span></div>
              <div className="flex gap-1.5"><div className="w-6 h-6 rounded-lg bg-muted"/><div className="w-6 h-6 rounded-lg bg-muted"/><div className="w-6 h-6 rounded-lg bg-muted"/></div>
            </div>
            <div className="flex" style={{height:220}}>
              {/* Sidebar */}
              <div className="w-40 border-r border-border p-3 flex flex-col gap-1">
                {["Home","Search","Library","Settings"].map((n,i)=><div key={n} className={cn("h-8 rounded-xl flex items-center px-3 text-xs font-semibold",i===0?"bg-primary/15 text-primary":"text-muted-foreground hover:bg-muted")}>{n}</div>)}
                <div className="mt-3 border-t border-border pt-3"><p className="text-[9px] font-bold uppercase tracking-widest text-muted-foreground px-2 mb-2">Design System</p>
                {["Foundation","Components","Patterns"].map(n=><div key={n} className="h-7 rounded-xl flex items-center px-3 text-[10px] font-semibold text-muted-foreground hover:bg-muted">{n}</div>)}
                </div>
              </div>
              {/* Content */}
              <div className="flex-1 p-4"><div className="h-full bg-muted/50 rounded-2xl flex items-center justify-center"><span className="text-xs text-muted-foreground">Content Area</span></div></div>
              {/* Right panel */}
              <div className="w-28 border-l border-border p-3"><p className="text-[10px] font-bold text-muted-foreground mb-2">Lyrics</p><div className="space-y-1.5">{[80,60,90,50,70].map((w,i)=><div key={i} className="h-2 rounded-full bg-muted" style={{width:`${w}%`}}/>)}</div></div>
            </div>
            {/* Mini player */}
            <div className="h-14 border-t border-border flex items-center px-4 gap-3 bg-card/80">
              <div className="w-9 h-9 rounded-xl" style={{background:`linear-gradient(135deg,${G[0][0]},${G[0][1]})`}}/>
              <div className="flex-1"><div className="h-2.5 bg-muted rounded-full w-32 mb-1"/><div className="h-2 bg-muted rounded-full w-20"/></div>
              <div className="flex gap-2">{[SkipBack,Play,SkipForward].map((Icon,i)=><div key={i} className="w-7 h-7 rounded-full bg-muted flex items-center justify-center"><Icon className="w-3.5 h-3.5 text-muted-foreground"/></div>)}</div>
              <div className="w-24 h-1.5 bg-muted rounded-full overflow-hidden"><div className="h-full w-2/5 rounded-full" style={{background:G[0][0]}}/></div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

function DSCompose() {
  const mappings = [
    {figma:"Button (Filled)",compose:"MiuixButton",module:"miuix.compose.ui.components",props:"text, onClick, enabled, colors"},
    {figma:"Settings Group Card",compose:"CardGroup",module:"miuix.compose.ui.layout",props:"title, items"},
    {figma:"Settings Item → Arrow",compose:"SuperArrow",module:"miuix.compose.ui.components",props:"title, summary, rightText, onClick"},
    {figma:"Navigation Rail",compose:"NavigationRail",module:"miuix.compose.ui.components",props:"items, selectedItem, onItemSelected"},
    {figma:"Navigation Bar",compose:"BottomNavBar",module:"miuix.compose.ui.components",props:"items, selectedItem, onItemSelected"},
    {figma:"Switch",compose:"MiuixSwitch",module:"miuix.compose.ui.components",props:"checked, onCheckedChange, enabled"},
    {figma:"Slider",compose:"MiuixSlider",module:"miuix.compose.ui.components",props:"value, onValueChange, valueRange"},
    {figma:"Top App Bar",compose:"DefaultTopAppBar / SmallTopAppBar",module:"miuix.compose.ui.components",props:"title, actions, navigationIcon"},
    {figma:"Scaffold",compose:"MiuixScaffold",module:"miuix.compose.ui.layout",props:"topBar, bottomBar, floatingActionButton, content"},
    {figma:"Dialog",compose:"MiuixDialog",module:"miuix.compose.ui.components",props:"title, summary, onDismiss, buttons"},
    {figma:"Small Title",compose:"SmallTitle",module:"miuix.compose.ui.components",props:"text, modifier"},
    {figma:"Page Navigator",compose:"Navigator",module:"miuix.compose.extra",props:"items, pageTransition, springSpec"},
    {figma:"Floating Card / Mini Player",compose:"FloatingCard",module:"miuix.compose.ui.layout",props:"content, modifier, elevation"},
    {figma:"Preference Item",compose:"Preference",module:"miuix.compose.ui.components",props:"title, summary, icon, trailing"},
  ];
  return (
    <div className="space-y-6 px-4 py-2 pb-8">
      <div className="bg-card/60 rounded-3xl border border-border p-5">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-10 h-10 rounded-2xl flex items-center justify-center" style={{background:"linear-gradient(135deg,var(--tide-green),var(--tide-blue))"}}><Code2 className="w-5 h-5 text-white"/></div>
          <div><p className="text-sm font-bold text-foreground">Compose Multiplatform Mapping</p><p className="text-xs text-muted-foreground">Figma → compose-miuix-ui 1:1</p></div>
        </div>
        <p className="text-xs text-muted-foreground">Each Figma component maps directly to a compose-miuix-ui component. The goal is Figma → Compose 1:1 so designers and developers share the same vocabulary.</p>
      </div>

      <div className="bg-card rounded-3xl border border-border overflow-hidden divide-y divide-border/60">
        <div className="grid grid-cols-[1fr,1fr] gap-4 px-4 py-2.5 bg-muted/50">
          <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Figma Component</p>
          <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Compose Component</p>
        </div>
        {mappings.map(m=>(
          <div key={m.figma} className="px-4 py-3 hover:bg-muted/30 transition-colors">
            <div className="grid grid-cols-[1fr,1fr] gap-4 mb-1.5">
              <p className="text-sm font-semibold text-foreground">{m.figma}</p>
              <code className="text-sm font-mono text-primary">{m.compose}</code>
            </div>
            <div className="grid grid-cols-[1fr,1fr] gap-4">
              <code className="text-[10px] font-mono text-muted-foreground">{m.module}</code>
              <p className="text-[10px] text-muted-foreground font-mono">{m.props}</p>
            </div>
          </div>
        ))}
      </div>

      <section>
        <SectionHeader title="Motion Mapping"/>
        <div className="bg-card rounded-3xl border border-border overflow-hidden divide-y divide-border/60">
          {[
            {figma:"Mini Player → Full Player",compose:"SharedTransitionScope + AnimatedContent",note:"Shared element with artwork key"},
            {figma:"Page transition",compose:"Navigator pageTransition + MiuixScrollBehavior",note:"Spring spec: stiffness=400, damping=35"},
            {figma:"Card hover / press",compose:"scale(0.97) via Indication",note:"rememberRipple or custom pressedScale"},
            {figma:"Blur Morph",compose:"BlurTransform + AnimatedBlur",note:"Compose 1.7+ BlurMask"},
          ].map(r=>(
            <div key={r.figma} className="px-4 py-3">
              <div className="flex items-start gap-3">
                <div className="w-1.5 h-1.5 rounded-full bg-primary mt-1.5 shrink-0"/>
                <div>
                  <p className="text-sm font-semibold text-foreground">{r.figma}</p>
                  <code className="text-xs font-mono text-secondary">{r.compose}</code>
                  <p className="text-xs text-muted-foreground mt-0.5">{r.note}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// NAVIGATION & TOOLBAR
// ─────────────────────────────────────────────────────────────
const APP_NAV = [
  {id:"cover" as Page,icon:Sparkles,label:"Cover"},
  {id:"home" as Page,icon:Home,label:"Home"},
  {id:"search" as Page,icon:Search,label:"Search"},
  {id:"library" as Page,icon:Library,label:"Library"},
  {id:"settings" as Page,icon:Settings,label:"Settings"},
];
const DS_NAV = [
  {id:"cover" as DSSection,icon:Sparkles,label:"Cover"},
  {id:"foundation" as DSSection,icon:Palette,label:"Foundation"},
  {id:"tokens" as DSSection,icon:Code2,label:"Tokens"},
  {id:"components" as DSSection,icon:Layers,label:"Components"},
  {id:"patterns" as DSSection,icon:LayoutDashboard,label:"Patterns"},
  {id:"compose" as DSSection,icon:Cpu,label:"Compose"},
];

function Sidebar({ page, onPage, dsSection, onDsSection, isDark, onToggleDark }: {
  page:Page; onPage:(p:Page)=>void; dsSection:DSSection; onDsSection:(s:DSSection)=>void; isDark:boolean; onToggleDark:()=>void;
}) {
  return (
    <aside className="hidden lg:flex flex-col w-56 shrink-0 bg-sidebar border-r border-sidebar-border h-full overflow-y-auto hide-scrollbar">
      {/* Logo */}
      <div className="flex items-center gap-3 px-4 pt-5 pb-4 shrink-0">
        <div className="w-8 h-8 rounded-xl flex items-center justify-center shrink-0" style={{background:"linear-gradient(135deg,var(--tide-pink),var(--tide-purple))"}}>
          <Music2 className="w-4 h-4 text-white"/>
        </div>
        <div><p className="text-sm font-black text-foreground tracking-tight leading-none">TideTunes</p><p className="text-[9px] text-muted-foreground font-medium">One Library. Every Source.</p></div>
      </div>
      {/* App Nav */}
      <div className="px-2 mb-1">
        <p className="text-[9px] font-bold uppercase tracking-widest text-muted-foreground px-2 mb-1.5">App</p>
        {APP_NAV.map(item=>{const Icon=item.icon; const active=page===item.id&&page!=="design-system";
          return <button key={item.id} onClick={()=>onPage(item.id)} className={cn("w-full flex items-center gap-2.5 px-2.5 py-2 rounded-xl mb-0.5 text-xs font-semibold transition-all",active?"bg-primary/15 text-primary":"text-sidebar-foreground hover:bg-sidebar-accent")}>
            <Icon style={{width:15,height:15}}/>{item.label}{active&&<div className="ml-auto w-1.5 h-1.5 rounded-full bg-primary"/>}
          </button>;
        })}
      </div>
      {/* DS Nav */}
      <div className="px-2 mt-2 mb-2">
        <p className="text-[9px] font-bold uppercase tracking-widest text-muted-foreground px-2 mb-1.5">Design System</p>
        {DS_NAV.map(item=>{const Icon=item.icon; const active=page==="design-system"&&dsSection===item.id;
          return <button key={item.id} onClick={()=>{onPage("design-system");onDsSection(item.id);}} className={cn("w-full flex items-center gap-2.5 px-2.5 py-2 rounded-xl mb-0.5 text-xs font-semibold transition-all",active?"bg-secondary/15 text-secondary":"text-sidebar-foreground hover:bg-sidebar-accent")}>
            <Icon style={{width:15,height:15}}/>{item.label}
          </button>;
        })}
      </div>
      <div className="flex-1"/>
      <div className="px-2 pb-4 shrink-0">
        <button onClick={onToggleDark} className="w-full flex items-center gap-2.5 px-2.5 py-2 rounded-xl bg-sidebar-accent hover:bg-muted transition-colors">
          {isDark?<Sun style={{width:15,height:15}} className="text-muted-foreground"/>:<Moon style={{width:15,height:15}} className="text-muted-foreground"/>}
          <span className="text-xs font-semibold text-sidebar-foreground">{isDark?"Light Mode":"Dark Mode"}</span>
        </button>
      </div>
    </aside>
  );
}

function DesktopToolbar({ page, dsSection, onDsSection, onPage, isDark, onToggleDark, rightPanel, onRightPanel }: {
  page:Page; dsSection:DSSection; onDsSection:(s:DSSection)=>void; onPage:(p:Page)=>void;
  isDark:boolean; onToggleDark:()=>void; rightPanel:RightPanel; onRightPanel:(p:RightPanel)=>void;
}) {
  return (
    <div className="hidden lg:flex items-center gap-3 px-4 h-12 border-b border-border bg-card/60 backdrop-blur-sm shrink-0">
      {/* Nav arrows */}
      <div className="flex gap-1">
        <button className="w-7 h-7 rounded-lg bg-muted flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors"><ArrowLeft className="w-3.5 h-3.5"/></button>
        <button className="w-7 h-7 rounded-lg bg-muted flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors"><ArrowRight className="w-3.5 h-3.5"/></button>
      </div>
      {/* Search */}
      <div className="flex-1 relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-muted-foreground"/>
        <input type="text" placeholder="Search TideTunes…" className="w-full h-8 pl-9 pr-3 bg-muted rounded-xl text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary/30"/>
      </div>
      {/* DS sub-nav pills */}
      {page==="design-system"&&(
        <div className="flex items-center gap-1">
          {DS_NAV.map(item=><button key={item.id} onClick={()=>onDsSection(item.id)} className={cn("h-7 px-3 rounded-lg text-[11px] font-semibold transition-all",dsSection===item.id?"bg-secondary text-secondary-foreground":"text-muted-foreground hover:text-foreground hover:bg-muted")}>{item.label}</button>)}
        </div>
      )}
      <div className="flex-1"/>
      {/* Right actions */}
      <div className="flex items-center gap-1">
        <button onClick={()=>onRightPanel(rightPanel==="lyrics"?null:"lyrics")} className={cn("w-8 h-8 rounded-lg flex items-center justify-center transition-colors",rightPanel==="lyrics"?"bg-primary/15 text-primary":"text-muted-foreground hover:text-foreground hover:bg-muted")}><AlignLeft className="w-4 h-4"/></button>
        <button onClick={()=>onRightPanel(rightPanel==="queue"?null:"queue")} className={cn("w-8 h-8 rounded-lg flex items-center justify-center transition-colors",rightPanel==="queue"?"bg-primary/15 text-primary":"text-muted-foreground hover:text-foreground hover:bg-muted")}><ListMusic className="w-4 h-4"/></button>
        <button onClick={onToggleDark} className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-colors">{isDark?<Sun className="w-4 h-4"/>:<Moon className="w-4 h-4"/>}</button>
        <button className="relative w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"><Bell className="w-4 h-4"/><span className="absolute top-1.5 right-1.5 w-1.5 h-1.5 rounded-full bg-primary"/></button>
      </div>
    </div>
  );
}

function BottomNav({ page, onPage }: { page:Page; onPage:(p:Page)=>void }) {
  const items = APP_NAV.filter(i=>i.id!=="cover");
  return (
    <nav className="lg:hidden flex items-center justify-around px-2 h-16 bg-card/80 backdrop-blur-xl border-t border-border shrink-0">
      {items.map(item=>{const Icon=item.icon; const active=page===item.id;
        return <button key={item.id} onClick={()=>onPage(item.id)} className={cn("flex flex-col items-center gap-0.5 px-3 py-1 rounded-2xl transition-all",active?"text-primary":"text-muted-foreground")}>
          <div className={cn("relative p-1.5 rounded-xl transition-all",active?"bg-primary/15":"")}>
            <Icon style={{width:20,height:20}}/>
            {active&&<motion.div layoutId="nav-dot" className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-primary"/>}
          </div>
          <span className="text-[10px] font-semibold">{item.label}</span>
        </button>;
      })}
    </nav>
  );
}

// ─────────────────────────────────────────────────────────────
// ROOT APP
// ─────────────────────────────────────────────────────────────
export default function App() {
  const [isDark,setIsDark] = useState(true);
  const [page,setPage] = useState<Page>("cover");
  const [dsSection,setDsSection] = useState<DSSection>("cover");
  const [currentSong,setCurrentSong] = useState<Song|null>(SONGS[0]);
  const [isPlaying,setIsPlaying] = useState(false);
  const [playerOpen,setPlayerOpen] = useState(false);
  const [progress,setProgress] = useState(40);
  const [volume,setVolume] = useState(75);
  const [songIdx,setSongIdx] = useState(0);
  const [rightPanel,setRightPanel] = useState<RightPanel>(null);

  const handlePlay = (song:Song) => { setCurrentSong(song); setIsPlaying(true); setSongIdx(SONGS.findIndex(s=>s.id===song.id)); };
  const handleNext = () => { const n=(songIdx+1)%SONGS.length; setSongIdx(n); setCurrentSong(SONGS[n]); setIsPlaying(true); };
  const handlePrev = () => { const p=(songIdx-1+SONGS.length)%SONGS.length; setSongIdx(p); setCurrentSong(SONGS[p]); setIsPlaying(true); };

  const mobilePageTitle: Partial<Record<Page,string>> = {
    cover:"TideTunes", home:"Good Evening", search:"Search", library:"Library", settings:"Settings",
  };
  const dsTitles: Record<DSSection,string> = {
    cover:"Design System",foundation:"Foundation",tokens:"Tokens",components:"Components",patterns:"Patterns",compose:"Compose",
  };

  return (
    <div className={cn("flex h-screen w-screen overflow-hidden",isDark?"dark":"")}>
      <div className="flex h-full w-full bg-background text-foreground overflow-hidden">
        <Sidebar page={page} onPage={setPage} dsSection={dsSection} onDsSection={setDsSection} isDark={isDark} onToggleDark={()=>setIsDark(!isDark)}/>

        <div className="flex flex-col flex-1 min-w-0 h-full overflow-hidden">
          {/* Desktop Toolbar */}
          <DesktopToolbar page={page} dsSection={dsSection} onDsSection={setDsSection} onPage={setPage} isDark={isDark} onToggleDark={()=>setIsDark(!isDark)} rightPanel={rightPanel} onRightPanel={setRightPanel}/>

          {/* Mobile top bar */}
          <header className="lg:hidden flex items-center justify-between px-5 pt-5 pb-3 shrink-0">
            <div>
              <h1 className="text-2xl font-black text-foreground">{page==="design-system"?dsTitles[dsSection]:mobilePageTitle[page]||"TideTunes"}</h1>
              {page==="design-system"&&<p className="text-xs text-muted-foreground mt-0.5">TideTunes DS · v3.0</p>}
            </div>
            <div className="flex items-center gap-2">
              <button onClick={()=>setIsDark(!isDark)} className="w-10 h-10 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground">
                {isDark?<Sun className="w-4 h-4"/>:<Moon className="w-4 h-4"/>}
              </button>
              <button className="w-10 h-10 rounded-2xl bg-muted flex items-center justify-center text-muted-foreground relative">
                <Bell className="w-4 h-4"/><span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-primary"/>
              </button>
            </div>
          </header>
          {/* Mobile DS sub-nav */}
          {page==="design-system"&&(
            <div className="lg:hidden px-4 mb-2 overflow-x-auto hide-scrollbar">
              <div className="flex gap-2">{DS_NAV.map(s=>(
                <button key={s.id} onClick={()=>setDsSection(s.id)} className={cn("shrink-0 px-3.5 h-8 rounded-full text-xs font-semibold transition-all",dsSection===s.id?"bg-secondary text-secondary-foreground":"bg-muted text-muted-foreground")}>{s.label}</button>
              ))}</div>
            </div>
          )}

          {/* Content + Right Panel */}
          <div className="flex flex-1 min-h-0 overflow-hidden">
            {/* Main content */}
            <main className="flex-1 overflow-y-auto">
              <AnimatePresence mode="wait">
                <motion.div key={page==="design-system"?`ds-${dsSection}`:page} initial={{opacity:0,y:8}} animate={{opacity:1,y:0}} exit={{opacity:0,y:-8}} transition={{duration:0.18,ease:"easeOut"}} className="min-h-full">
                  {page==="cover"&&<CoverPage onEnter={()=>setPage("home")}/>}
                  {page==="home"&&<HomePage onPlay={handlePlay}/>}
                  {page==="search"&&<SearchPage onPlay={handlePlay}/>}
                  {page==="library"&&<LibraryPage onPlay={handlePlay}/>}
                  {page==="settings"&&<SettingsPage/>}
                  {page==="design-system"&&dsSection==="cover"&&<DSCover/>}
                  {page==="design-system"&&dsSection==="foundation"&&<DSFoundation/>}
                  {page==="design-system"&&dsSection==="tokens"&&<DSTokens/>}
                  {page==="design-system"&&dsSection==="components"&&<DSComponents/>}
                  {page==="design-system"&&dsSection==="patterns"&&<DSPatterns/>}
                  {page==="design-system"&&dsSection==="compose"&&<DSCompose/>}
                </motion.div>
              </AnimatePresence>
            </main>

            {/* Desktop right panel */}
            <AnimatePresence>
              {rightPanel&&<RightPanelView panel={rightPanel} song={currentSong} onClose={()=>setRightPanel(null)}/>}
            </AnimatePresence>
          </div>

          {/* Mini Player */}
          <AnimatePresence>
            {currentSong&&page!=="cover"&&<MiniPlayer song={currentSong} isPlaying={isPlaying} onPlayPause={()=>setIsPlaying(!isPlaying)} onNext={handleNext} onExpand={()=>setPlayerOpen(true)}/>}
          </AnimatePresence>

          {/* Bottom Nav */}
          <BottomNav page={page} onPage={setPage}/>
        </div>
      </div>

      {/* Full Player */}
      <AnimatePresence>
        {playerOpen&&currentSong&&<FullPlayer song={currentSong} isPlaying={isPlaying} onPlayPause={()=>setIsPlaying(!isPlaying)} onNext={handleNext} onPrev={handlePrev} onClose={()=>setPlayerOpen(false)} progress={progress} onSeek={setProgress} volume={volume} onVolume={setVolume}/>}
      </AnimatePresence>
    </div>
  );
}
