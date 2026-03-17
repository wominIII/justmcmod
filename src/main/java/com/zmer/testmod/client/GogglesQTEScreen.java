package com.zmer.testmod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.*;

/**
 * "Neural Desync Protocol — Pipe Network"
 *
 * A pipe-rotation puzzle the player must solve to remove the Wireframe Goggles.
 * The system actively resists the player's attempts with multiple anti-hack mechanisms.
 *
 * ALL rendering uses GuiGraphics.fill() only — no Tesselator / raw GL.
 */
public class GogglesQTEScreen extends Screen {

    // ── Grid ─────────────────────────────────────────────────
    private static final int GRID_SIZE = 7;
    private static final int CELL_SIZE = 30;
    private static final int PIPE_HALF = 4;
    private static final long TIME_LIMIT_MS = 40_000;  // 40 seconds

    // Directions
    private static final int UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3;
    private static final int[] DR = {-1, 0, 1, 0};
    private static final int[] DC = {0, 1, 0, -1};

    private enum Pipe { EMPTY, STRAIGHT, CORNER, T_PIECE, CROSS }

    // ── Anti-hack difficulty settings (normal mode) ───────────
    /** Chance (0..1) that clicking a pipe triggers a nearby scramble. */
    private static final float SCRAMBLE_CHANCE = 0.25f;
    /** How many nearby pipes get scrambled on trigger. */
    private static final int SCRAMBLE_COUNT = 2;
    /** Interval (ms) between automatic "virus pulse" disruptions. */
    private static final long VIRUS_INTERVAL_MS = 6_000;
    /** Number of random pipes rotated by each virus pulse. */
    private static final int VIRUS_ROTATE_COUNT = 3;
    /** EMP blackout duration (ms) — hides all pipe colors. */
    private static final long EMP_DURATION_MS = 1_200;
    /** Some cells are "firewalled" and need 2 clicks to actually rotate. */
    private static final float FIREWALL_CHANCE = 0.18f;

    // ── Nightmare mode settings (collar owned by another player) ───────────
    /** Nightmare mode: scramble chance increased to 70% */
    private static final float NIGHTMARE_SCRAMBLE_CHANCE = 0.70f;
    /** Nightmare mode: scramble 5 nearby pipes instead of 2 */
    private static final int NIGHTMARE_SCRAMBLE_COUNT = 5;
    /** Nightmare mode: virus pulse every 2 seconds */
    private static final long NIGHTMARE_VIRUS_INTERVAL_MS = 2_000;
    /** Nightmare mode: virus rotates 8 pipes instead of 3 */
    private static final int NIGHTMARE_VIRUS_ROTATE_COUNT = 8;
    /** Nightmare mode: EMP lasts 3 seconds */
    private static final long NIGHTMARE_EMP_DURATION_MS = 3_000;
    /** Nightmare mode: 50% of cells are firewalled */
    private static final float NIGHTMARE_FIREWALL_CHANCE = 0.50f;
    /** Nightmare mode: time limit is only 20 seconds */
    private static final long NIGHTMARE_TIME_LIMIT_MS = 20_000;

    // ── State ────────────────────────────────────────────────
    private Pipe[][] grid;
    private int[][] rot;
    private boolean[][] connected;
    private boolean[][] firewalled;
    private int[][] firewallClicks;
    private int sourceR, sourceC, sinkR, sinkC;

    private long startTime;
    private boolean solved, failed;
    private long resultTime;

    private int gridX, gridY;
    private int hoverR = -1, hoverC = -1;
    private final Random rand = new Random();

    // Anti-hack runtime state
    private long lastVirusPulse;
    private long empStartTime;
    private String warningMsg = null;
    private long warningStart = 0;
    private int totalClicks = 0;

    // ── Colors ───────────────────────────────────────────────
    private static final int COL_BG         = 0xE0080812;
    private static final int COL_CELL       = 0xFF12122A;
    private static final int COL_CELL_HOVER = 0xFF1E1E3A;
    private static final int COL_BORDER     = 0xFF2A2A44;
    private static final int COL_PIPE       = 0xFF555577;
    private static final int COL_PIPE_EMP   = 0xFF222233;
    private static final int COL_FIREWALL   = 0xFFAA3333;
    private static final int COL_SOURCE     = 0xFF00FFFF;
    private static final int COL_SINK       = 0xFFFF66CC;
    private static final int COL_TITLE      = 0xFFFF66CC;
    private static final int COL_TIMER      = 0xFFFFAA00;
    private static final int COL_HIT        = 0xFF00FF00;
    private static final int COL_MISS       = 0xFFFF0000;
    private static final int COL_WARN       = 0xFFFF4444;
    private static final int COL_TEXT       = 0xFFBBBBBB;
    private static final int COL_SCANLINE   = 0x0CFFFFFF;

    /** Callback executed when the puzzle is solved. */
    private final Runnable onSolvedCallback;

    /** If true, the puzzle is nearly impossible to solve (collar owned by another player). */
    private final boolean nightmareMode;

    /** Default constructor — does nothing on solve by default, usually not used natively without lambda anymore. */
    public GogglesQTEScreen() {
        this(() -> {}, false);
    }

    /** Parameterised constructor — custom callback on success (used by collar, etc.). */
    public GogglesQTEScreen(Runnable onSolved) {
        this(onSolved, false);
    }

    /** Full constructor with nightmare mode support. */
    public GogglesQTEScreen(Runnable onSolved, boolean nightmareMode) {
        super(Component.literal("Neural Desync Protocol"));
        this.onSolvedCallback = onSolved;
        this.nightmareMode = nightmareMode;
    }

    /* ══════════════  INIT  ══════════════ */

    @Override
    protected void init() {
        super.init();
        solved = failed = false;
        resultTime = 0;
        totalClicks = 0;
        empStartTime = 0;
        warningMsg = null;
        startTime = System.currentTimeMillis();
        lastVirusPulse = startTime;
        generatePuzzle();
        initFirewalls();
        updateConnections();
        gridX = (this.width  - GRID_SIZE * CELL_SIZE) / 2;
        gridY = (this.height - GRID_SIZE * CELL_SIZE) / 2 + 14;
    }

    /** Mark random cells as firewalled (need extra clicks to rotate). */
    private void initFirewalls() {
        firewalled = new boolean[GRID_SIZE][GRID_SIZE];
        firewallClicks = new int[GRID_SIZE][GRID_SIZE];
        float chance = nightmareMode ? NIGHTMARE_FIREWALL_CHANCE : FIREWALL_CHANCE;
        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++) {
                if ((r == sourceR && c == sourceC) || (r == sinkR && c == sinkC)) continue;
                if (rand.nextFloat() < chance) {
                    firewalled[r][c] = true;
                    firewallClicks[r][c] = 1;
                }
            }
    }

    /* ══════════════  PUZZLE GENERATION  ══════════════ */

    @SuppressWarnings("unchecked")
    private void generatePuzzle() {
        grid = new Pipe[GRID_SIZE][GRID_SIZE];
        rot  = new int[GRID_SIZE][GRID_SIZE];
        for (Pipe[] row : grid) Arrays.fill(row, Pipe.EMPTY);

        Set<Integer>[][] need = new HashSet[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                need[r][c] = new HashSet<>();

        sourceR = 0; sourceC = 0;
        sinkR = GRID_SIZE - 1; sinkC = GRID_SIZE - 1;

        // 1) Main path (DFS backtracking)
        boolean[][] vis = new boolean[GRID_SIZE][GRID_SIZE];
        List<int[]> path = new ArrayList<>();
        dfs(sourceR, sourceC, sinkR, sinkC, vis, path);

        for (int i = 0; i < path.size(); i++) {
            int r = path.get(i)[0], c = path.get(i)[1];
            if (i > 0) need[r][c].add(dirTo(r, c, path.get(i - 1)[0], path.get(i - 1)[1]));
            if (i < path.size() - 1) need[r][c].add(dirTo(r, c, path.get(i + 1)[0], path.get(i + 1)[1]));
        }

        // 2) Branch paths (dead-end distractions)
        for (int b = 0; b < 4; b++) {
            int[] start = path.get(rand.nextInt(path.size()));
            extendBranch(start[0], start[1], vis, need, 3 + rand.nextInt(4));
        }

        // 3) Assign pipe types from needed directions
        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                if (!need[r][c].isEmpty()) assignPipe(r, c, need[r][c]);

        // 4) Fill rest with random pipes (no CROSS)
        Pipe[] fillers = {Pipe.STRAIGHT, Pipe.CORNER, Pipe.T_PIECE};
        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                if (grid[r][c] == Pipe.EMPTY) {
                    grid[r][c] = fillers[rand.nextInt(fillers.length)];
                    rot[r][c]  = rand.nextInt(4);
                }

        // 5) Save solution then scramble (guarantee unsolved)
        int[][] sol = new int[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) sol[r] = rot[r].clone();
        int attempts = 0;
        do {
            for (int r = 0; r < GRID_SIZE; r++)
                for (int c = 0; c < GRID_SIZE; c++)
                    rot[r][c] = (sol[r][c] + 1 + rand.nextInt(3)) % 4;
            updateConnections();
            if (++attempts > 200) break;
        } while (connected[sinkR][sinkC]);
    }

    private boolean dfs(int r, int c, int er, int ec, boolean[][] vis, List<int[]> path) {
        vis[r][c] = true;
        path.add(new int[]{r, c});
        if (r == er && c == ec) return true;
        List<Integer> dirs = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(dirs, rand);
        for (int d : dirs) {
            int nr = r + DR[d], nc = c + DC[d];
            if (ok(nr, nc) && !vis[nr][nc] && dfs(nr, nc, er, ec, vis, path)) return true;
        }
        path.remove(path.size() - 1);
        vis[r][c] = false;
        return false;
    }

    private void extendBranch(int r, int c, boolean[][] vis,
                              Set<Integer>[][] need, int maxLen) {
        for (int i = 0; i < maxLen; i++) {
            List<Integer> dirs = new ArrayList<>(List.of(0, 1, 2, 3));
            Collections.shuffle(dirs, rand);
            boolean moved = false;
            for (int d : dirs) {
                int nr = r + DR[d], nc = c + DC[d];
                if (ok(nr, nc) && !vis[nr][nc]) {
                    vis[nr][nc] = true;
                    need[r][c].add(d);
                    need[nr][nc].add((d + 2) % 4);
                    r = nr; c = nc;
                    moved = true;
                    break;
                }
            }
            if (!moved) break;
        }
    }

    private int dirTo(int r, int c, int tr, int tc) {
        if (tr < r) return UP;   if (tr > r) return DOWN;
        if (tc > c) return RIGHT; return LEFT;
    }

    private void assignPipe(int r, int c, Set<Integer> needed) {
        int n = needed.size();
        if (n >= 4) { grid[r][c] = Pipe.CROSS; rot[r][c] = 0; return; }
        if (n == 3) {
            grid[r][c] = Pipe.T_PIECE;
            for (int d = 0; d < 4; d++)
                if (!needed.contains(d)) { rot[r][c] = (d + 1) % 4; break; }
            return;
        }
        if (n == 2) {
            Integer[] d = needed.toArray(new Integer[0]);
            if ((d[0] + 2) % 4 == d[1]) {
                grid[r][c] = Pipe.STRAIGHT;
                rot[r][c] = d[0] % 2;
            } else {
                grid[r][c] = Pipe.CORNER;
                for (int rr = 0; rr < 4; rr++)
                    if (needed.contains(rr) && needed.contains((rr + 1) % 4)) { rot[r][c] = rr; break; }
            }
            return;
        }
        int dir = needed.iterator().next();
        grid[r][c] = Pipe.STRAIGHT;
        rot[r][c] = dir % 2;
    }

    /* ══════════════  CONNECTIVITY  ══════════════ */

    private void updateConnections() {
        connected = new boolean[GRID_SIZE][GRID_SIZE];
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sourceR, sourceC});
        connected[sourceR][sourceC] = true;
        while (!q.isEmpty()) {
            int[] cell = q.poll();
            int cr = cell[0], cc = cell[1];
            for (int dir : openDirs(cr, cc)) {
                int nr = cr + DR[dir], nc = cc + DC[dir];
                if (ok(nr, nc) && !connected[nr][nc] && openDirs(nr, nc).contains((dir + 2) % 4)) {
                    connected[nr][nc] = true;
                    q.add(new int[]{nr, nc});
                }
            }
        }
    }

    private Set<Integer> openDirs(int r, int c) {
        Set<Integer> base = new HashSet<>();
        switch (grid[r][c]) {
            case STRAIGHT: base.add(UP); base.add(DOWN); break;
            case CORNER:   base.add(UP); base.add(RIGHT); break;
            case T_PIECE:  base.add(UP); base.add(RIGHT); base.add(DOWN); break;
            case CROSS:    base.add(UP); base.add(RIGHT); base.add(DOWN); base.add(LEFT); break;
            default:       return base;
        }
        Set<Integer> out = new HashSet<>();
        for (int d : base) out.add((d + rot[r][c]) % 4);
        return out;
    }

    private boolean ok(int r, int c) { return r >= 0 && r < GRID_SIZE && c >= 0 && c < GRID_SIZE; }

    /* ══════════════  INPUT  ══════════════ */

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (solved || failed) return super.mouseClicked(mx, my, btn);
        int col = (int) ((mx - gridX) / CELL_SIZE);
        int row = (int) ((my - gridY) / CELL_SIZE);
        if (ok(row, col)) {
            totalClicks++;

            // ── Firewall check: absorb click if cell still has firewall HP ──
            if (firewalled[row][col] && firewallClicks[row][col] > 0) {
                firewallClicks[row][col]--;
                showWarning("\u26A0 \u9632\u706B\u5899\u62E6\u622A \u2014 \u518D\u70B9\u51FB\u4E00\u6B21");
                return true;
            }
            // Reset firewall for next time this cell is clicked
            if (firewalled[row][col]) firewallClicks[row][col] = 1;

            // ── Actual rotation ──
            rot[row][col] = (rot[row][col] + (btn == 1 ? 3 : 1)) % 4;

            // ── Anti-hack: chance to scramble nearby pipes ──
            float scrambleChance = nightmareMode ? NIGHTMARE_SCRAMBLE_CHANCE : SCRAMBLE_CHANCE;
            if (rand.nextFloat() < scrambleChance) {
                scrambleNearby(row, col);
                showWarning("\u26A1 \u7CFB\u7EDF\u53CD\u5236 \u2014 \u7BA1\u9053\u88AB\u6253\u4E71");
            }

            // ── Escalation: after many clicks, trigger EMP blackout ──
            int empTrigger = nightmareMode ? 8 : 15;  // More frequent EMP in nightmare
            if (totalClicks > 0 && totalClicks % empTrigger == 0) {
                empStartTime = System.currentTimeMillis();
                showWarning("\u2622 EMP\u8109\u51B2 \u2014 \u4FE1\u53F7\u4E2D\u65AD");
            }

            updateConnections();
            if (connected[sinkR][sinkC]) {
                solved = true;
                resultTime = System.currentTimeMillis();
                onSolvedCallback.run();
            }
        }
        return true;
    }

    /** Scramble SCRAMBLE_COUNT random pipes adjacent to (row,col). */
    private void scrambleNearby(int row, int col) {
        List<int[]> neighbors = new ArrayList<>();
        for (int d = 0; d < 4; d++) {
            int nr = row + DR[d], nc = col + DC[d];
            if (ok(nr, nc) && !(nr == sourceR && nc == sourceC) && !(nr == sinkR && nc == sinkC))
                neighbors.add(new int[]{nr, nc});
        }
        int[][] diag = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] dd : diag) {
            int nr = row + dd[0], nc = col + dd[1];
            if (ok(nr, nc)) neighbors.add(new int[]{nr, nc});
        }
        Collections.shuffle(neighbors, rand);
        int count = nightmareMode ? NIGHTMARE_SCRAMBLE_COUNT : SCRAMBLE_COUNT;
        count = Math.min(count, neighbors.size());
        for (int i = 0; i < count; i++) {
            int[] n = neighbors.get(i);
            rot[n[0]][n[1]] = (rot[n[0]][n[1]] + 1 + rand.nextInt(3)) % 4;
        }
    }

    /** Fire a periodic "virus pulse" that rotates random pipes. */
    private void virusPulse() {
        List<int[]> candidates = new ArrayList<>();
        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                if (!(r == sourceR && c == sourceC) && !(r == sinkR && c == sinkC))
                    candidates.add(new int[]{r, c});
        Collections.shuffle(candidates, rand);
        int count = nightmareMode ? NIGHTMARE_VIRUS_ROTATE_COUNT : VIRUS_ROTATE_COUNT;
        count = Math.min(count, candidates.size());
        for (int i = 0; i < count; i++) {
            int[] cell = candidates.get(i);
            rot[cell[0]][cell[1]] = (rot[cell[0]][cell[1]] + 1 + rand.nextInt(2)) % 4;
        }
        updateConnections();
        showWarning("\u2623 \u75C5\u6BD2\u8109\u51B2 \u2014 \u7BA1\u9053\u88AB\u7BE1\u6539");
    }

    private void showWarning(String msg) {
        warningMsg = msg;
        warningStart = System.currentTimeMillis();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 256) { onClose(); return true; }
        return super.keyPressed(key, scan, mod);
    }

    /* ══════════════  RENDER  ══════════════ */

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();

        // ── Time check ──
        long timeLimit = nightmareMode ? NIGHTMARE_TIME_LIMIT_MS : TIME_LIMIT_MS;
        if (!solved && !failed && now - startTime >= timeLimit) { failed = true; resultTime = now; }
        if ((solved || failed) && resultTime > 0 && now - resultTime > 2200) { onClose(); return; }

        // ── Virus pulse (periodic auto-disruption) ──
        long virusInterval = nightmareMode ? NIGHTMARE_VIRUS_INTERVAL_MS : VIRUS_INTERVAL_MS;
        if (!solved && !failed && now - lastVirusPulse >= virusInterval) {
            lastVirusPulse = now;
            virusPulse();
        }

        long empDuration = nightmareMode ? NIGHTMARE_EMP_DURATION_MS : EMP_DURATION_MS;
        boolean empActive = empStartTime > 0 && now - empStartTime < empDuration;

        int w = this.width, h = this.height, cxScreen = w / 2;
        gridX = (w - GRID_SIZE * CELL_SIZE) / 2;
        gridY = (h - GRID_SIZE * CELL_SIZE) / 2 + 14;

        // Background
        g.fill(0, 0, w, h, COL_BG);
        if (!empActive || (now / 80) % 2 == 0) {
            for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, COL_SCANLINE);
        }

        // Title
        int titleCol = empActive ? ((now / 100) % 2 == 0 ? COL_WARN : COL_TITLE) : COL_TITLE;
        g.drawCenteredString(this.font, "\u25C8 \u795E\u7ECF\u65AD\u8054\u534F\u8BAE \u2014 \u7BA1\u9053\u7F51\u7EDC \u25C8",
                cxScreen, 6, titleCol);

        // Timer bar
        if (!solved && !failed) {
            long el = now - startTime;
            float frac = 1f - Mth.clamp((float)el / TIME_LIMIT_MS, 0, 1);
            int barW = GRID_SIZE * CELL_SIZE, barX = gridX, barY = gridY - 14;
            g.fill(barX, barY, barX + barW, barY + 5, 0xFF222233);
            g.fill(barX, barY, barX + (int)(barW * frac), barY + 5, frac < 0.2f ? COL_MISS : COL_TIMER);
            g.drawString(this.font, String.format("%.1fs", (TIME_LIMIT_MS - el) / 1000.0),
                    barX + barW + 5, barY - 1, COL_TIMER);
        }

        // Hover
        hoverR = (int)((my - gridY) / (float) CELL_SIZE);
        hoverC = (int)((mx - gridX) / (float) CELL_SIZE);
        if (!ok(hoverR, hoverC)) { hoverR = hoverC = -1; }

        float pulse = (Mth.sin((now % 1200) / 1200f * (float)(2 * Math.PI)) + 1f) / 2f;

        // Grid cells
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                int x0 = gridX + c * CELL_SIZE, y0 = gridY + r * CELL_SIZE;
                boolean hover = (r == hoverR && c == hoverC && !solved && !failed);
                g.fill(x0, y0, x0 + CELL_SIZE, y0 + CELL_SIZE, hover ? COL_CELL_HOVER : COL_CELL);
                if (r == sourceR && c == sourceC)
                    g.fill(x0 + 1, y0 + 1, x0 + CELL_SIZE - 1, y0 + CELL_SIZE - 1, 0x2200FFFF);
                if (r == sinkR && c == sinkC)
                    g.fill(x0 + 1, y0 + 1, x0 + CELL_SIZE - 1, y0 + CELL_SIZE - 1, 0x22FF66CC);

                // Firewall tint (red border glow)
                if (firewalled[r][c] && firewallClicks[r][c] > 0) {
                    g.fill(x0 + 1, y0 + 1, x0 + CELL_SIZE - 1, y0 + 2, COL_FIREWALL);
                    g.fill(x0 + 1, y0 + CELL_SIZE - 2, x0 + CELL_SIZE - 1, y0 + CELL_SIZE - 1, COL_FIREWALL);
                    g.fill(x0 + 1, y0 + 1, x0 + 2, y0 + CELL_SIZE - 1, COL_FIREWALL);
                    g.fill(x0 + CELL_SIZE - 2, y0 + 1, x0 + CELL_SIZE - 1, y0 + CELL_SIZE - 1, COL_FIREWALL);
                }

                // Normal border
                g.fill(x0, y0, x0 + CELL_SIZE, y0 + 1, COL_BORDER);
                g.fill(x0, y0 + CELL_SIZE - 1, x0 + CELL_SIZE, y0 + CELL_SIZE, COL_BORDER);
                g.fill(x0, y0, x0 + 1, y0 + CELL_SIZE, COL_BORDER);
                g.fill(x0 + CELL_SIZE - 1, y0, x0 + CELL_SIZE, y0 + CELL_SIZE, COL_BORDER);

                // Pipe (hidden during EMP)
                drawPipe(g, x0, y0, r, c, pulse, empActive);
            }
        }

        // S / D labels
        drawLabel(g, sourceR, sourceC, "S", COL_SOURCE);
        drawLabel(g, sinkR,   sinkC,   "D", COL_SINK);

        // Instructions
        int iy = gridY + GRID_SIZE * CELL_SIZE + 8;
        g.drawCenteredString(this.font, "\u5DE6\u952E\u65CB\u8F6C | \u53F3\u952E\u53CD\u8F6C | \u8FDE\u901A [S]\u2192[D]",
                cxScreen, iy, COL_TEXT);

        // Warning message (anti-hack events)
        if (warningMsg != null && now - warningStart < 1500) {
            g.drawCenteredString(this.font, warningMsg, cxScreen, iy + 14, COL_WARN);
        }

        // EMP overlay (dark static)
        if (empActive) {
            float empProg = (now - empStartTime) / (float) EMP_DURATION_MS;
            int overlayAlpha = (int)((1f - empProg) * 0x88);
            g.fill(gridX, gridY, gridX + GRID_SIZE * CELL_SIZE,
                    gridY + GRID_SIZE * CELL_SIZE, (overlayAlpha << 24) | 0x000811);
            g.drawCenteredString(this.font, "\u2588\u2588 SIGNAL LOST \u2588\u2588",
                    cxScreen, gridY + GRID_SIZE * CELL_SIZE / 2, 0xFFFF2222);
        }

        // Result
        int resultY = iy + (warningMsg != null && now - warningStart < 1500 ? 28 : 14);
        if (solved)
            g.drawCenteredString(this.font, "\u2713 \u65AD\u8054\u6210\u529F \u2014 \u62A4\u76EE\u955C\u5DF2\u89E3\u9501",
                    cxScreen, resultY, COL_HIT);
        else if (failed)
            g.drawCenteredString(this.font, "\u2716 \u534F\u8BAE\u8D85\u65F6 \u2014 \u62A4\u76EE\u955C\u4FDD\u6301\u9501\u5B9A",
                    cxScreen, resultY, COL_MISS);

        // Decorative corners
        int cL = 14;
        g.fill(4, 4, 4+cL, 5, COL_TITLE); g.fill(4, 4, 5, 4+cL, COL_TITLE);
        g.fill(w-4-cL, 4, w-4, 5, COL_TITLE); g.fill(w-5, 4, w-4, 4+cL, COL_TITLE);
        g.fill(4, h-5, 4+cL, h-4, COL_TITLE); g.fill(4, h-4-cL, 5, h-4, COL_TITLE);
        g.fill(w-4-cL, h-5, w-4, h-4, COL_TITLE); g.fill(w-5, h-4-cL, w-4, h-4, COL_TITLE);

        super.render(g, mx, my, pt);
    }

    /** Draw pipe segments inside a cell using fill() only. */
    private void drawPipe(GuiGraphics g, int x0, int y0, int row, int col, float pulse, boolean empActive) {
        int half = CELL_SIZE / 2;
        int px = x0 + half, py = y0 + half;
        boolean conn = connected != null && connected[row][col];

        int pipeCol;
        if (empActive) {
            pipeCol = COL_PIPE_EMP;
        } else if (conn) {
            int green = (int)(0xAA + 0x55 * pulse);
            pipeCol = 0xFF000000 | (green << 8) | 0x66;
        } else {
            pipeCol = COL_PIPE;
        }

        Set<Integer> dirs = openDirs(row, col);
        if (dirs.isEmpty()) return;
        int pw = PIPE_HALF;
        for (int dir : dirs) {
            switch (dir) {
                case UP:    g.fill(px-pw, y0,      px+pw, py+pw, pipeCol); break;
                case DOWN:  g.fill(px-pw, py-pw,   px+pw, y0+CELL_SIZE, pipeCol); break;
                case LEFT:  g.fill(x0,    py-pw,   px+pw, py+pw, pipeCol); break;
                case RIGHT: g.fill(px-pw, py-pw,   x0+CELL_SIZE, py+pw, pipeCol); break;
            }
        }
        g.fill(px-pw, py-pw, px+pw, py+pw, pipeCol); // center joint
        if (conn && !empActive) g.fill(px-1, py-1, px+2, py+2, 0xFFFFFFFF); // bright dot
    }

    private void drawLabel(GuiGraphics g, int row, int col, String text, int color) {
        int x = gridX + col * CELL_SIZE + CELL_SIZE / 2;
        int y = gridY + row * CELL_SIZE + CELL_SIZE / 2 - 4;
        g.drawCenteredString(this.font, text, x + 1, y + 1, 0xFF000000);
        g.drawCenteredString(this.font, text, x, y, color);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
