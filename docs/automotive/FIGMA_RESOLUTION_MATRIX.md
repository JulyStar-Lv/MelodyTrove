# Figma Automotive full-file inventory and resolution matrix

Source file `5NO5nvLFdd2jPTwHmnJE8A` was read through Figma MCP on 2026-09-10. The file has one page (`0:1`, `Page 1`), no Section nodes, 53 top-level Frames, 89 local component definitions (`symbol` in MCP metadata), and 950 component instances. Every formal screen row below was read by actual node ID with `get_design_context`; each response included the rendered screenshot and component/layout hierarchy. Variable reads returned no local variables for the primary screen roots; Extended States exposes the `--tide-*` surface/text tokens recorded in the design.

## Top-level frame inventory

The 53 top-level frames comprise 20 formal 2496×1080 application screens (10 Dark/Light pairs), 20 formal 1728×1080 application screens, two 2496 Extended States frames, three 5120×1440 Automotive context/fullscreen frames, one 1200×375 layout specification, six design-system/component/state boards, and one 15760×5816 Cover Flow construction board. The construction/design-system boards are implementation references rather than app-window acceptance targets.

## Formal target matrix

| Profile candidate | Resolution | Screen/state | Theme | Figma node | Required |
|---|---:|---|---|---|---|
| Expanded | 2496×1080 | Home | Dark / Light | `1:2` / `89:480` | YES |
| Expanded | 2496×1080 | Now Playing | Dark / Light | `1:65` / `89:566` | YES |
| Expanded | 2496×1080 | Queue state | Dark / Light | `1:122` / `89:642` | YES |
| Expanded | 2496×1080 | Library · Songs | Dark / Light | `3:384` / `89:845` | YES |
| Expanded | 2496×1080 | Settings | Dark / Light | `3:463` / `89:965` | YES |
| Expanded | 2496×1080 | Playlists | Dark / Light | `970:776` / `970:1777` | YES |
| Expanded | 2496×1080 | Library · Albums | Dark / Light | `970:1176` / `970:2177` | YES |
| Expanded | 2496×1080 | Library · Artists | Dark / Light | `970:1376` / `970:2377` | YES |
| Expanded | 2496×1080 | Album detail | Dark / Light | `977:944` / `979:1012` | YES |
| Expanded | 2496×1080 | Artist detail | Dark / Light | `977:1189` / `979:1257` | YES |
| Expanded | 2496×1080 | Search/Dialog/Loading/Empty/Error/interaction states | Dark / Light | `1922:1508` / `1927:1508` | YES |
| VehiclePanel | 1728×1080 | Home | Dark / Light | `1877:1508` / `1886:2170` | YES |
| VehiclePanel | 1728×1080 | Now Playing | Dark / Light | `1886:2376` / `1886:2808` | YES |
| VehiclePanel | 1728×1080 | Queue state | Dark / Light | `1886:2602` / `1886:3034` | YES |
| VehiclePanel | 1728×1080 | Library · Songs | Dark / Light | `1879:1516` / `1879:2469` | YES |
| VehiclePanel | 1728×1080 | Settings | Dark / Light | `1879:1808` / `1879:2761` | YES |
| VehiclePanel | 1728×1080 | Playlists | Dark / Light | `1879:1909` / `1879:2862` | YES |
| VehiclePanel | 1728×1080 | Library · Albums | Dark / Light | `1879:2188` / `1879:3141` | YES |
| VehiclePanel | 1728×1080 | Library · Artists | Dark / Light | `1879:2334` / `1879:3287` | YES |
| VehiclePanel | 1728×1080 | Album detail | Dark / Light | `1886:3240` / `1886:3659` | YES |
| VehiclePanel | 1728×1080 | Artist detail | Dark / Light | `1886:3468` / `1886:3887` | YES |
| FullscreenCockpit | 5120×1304 content in 5120×1440 display | Minimal Now Playing | Dark | `1480:1130` (`1480:1131` content) | YES |
| FullscreenCockpit | 5120×1304 content in 5120×1440 display | Cover Flow | Dark | `1524:1130` | YES |

## Reference-only top-level boards

`3:40`, `90:818`, `3:139`, `3:523`, `90:1097`, and `95:1010` define components, design tokens, interaction states, and colors. `727:772` shows a 5120×1440 vehicle desktop containing a 2496×1080 Tide Player window; it proves that 5120 physical display size is distinct from a normal app-window profile. `1933:1508` explicitly defines Expanded 2496×1080 and Vehicle Visible Area 1728×1080. `1726:1417` is the component construction canvas used by the formal Cover Flow frame.

## Layout differences established from node metadata

| Metric/structure | Expanded | VehiclePanel | FullscreenCockpit |
|---|---:|---:|---:|
| Navigation rail | x40, 352 wide | x24, 240 wide | absent |
| App content start | 416 | 280 | full surface |
| Library/settings control pane | 560 | 480 | N/A |
| Library/settings content pane | 1424 | 920–944 | N/A |
| Album/artist grid | 4 columns | 3 columns | N/A |
| Now Playing player pane | 1000 | 660 | 720 artwork + lyrics region |
| Now Playing secondary pane | 1328 | 848/960 | structural fullscreen layout |
| Artwork | 640 | 600 | 720 |

No formal Search page exists at 1728 or 2496; Search is specified as a component/state family in Extended States. The shared Automotive Search route implements that contract at both app-window profiles. Playlist detail likewise uses the Playlists two-pane contract because Figma supplies the list and selected-track panel as one formal frame.
