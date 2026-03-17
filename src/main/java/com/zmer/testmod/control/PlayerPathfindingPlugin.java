package com.zmer.testmod.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight pathfinding plugin for player movement tasks.
 * Uses A* over walkable block positions and drives movement without teleporting.
 */
public final class PlayerPathfindingPlugin {

    private static final int REPATH_INTERVAL_TICKS = 20;
    private static final int MAX_SEARCH_NODES = 4000;
    private static final int MAX_RANGE_FROM_START = 96;

    private static final Map<UUID, PathState> STATES = new ConcurrentHashMap<>();

    private PlayerPathfindingPlugin() {}

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    public static boolean tickMoveTo(ServerPlayer player, BlockPos goal, int serverTick) {
        if (!(player.level() instanceof ServerLevel level)) return false;

        PathState state = STATES.computeIfAbsent(player.getUUID(), k -> new PathState());
        BlockPos currentFeet = player.blockPosition();

        if (needsRepath(state, currentFeet, goal, serverTick)) {
            state.path = findPath(level, currentFeet, goal);
            state.nextIndex = 0;
            state.goal = goal.immutable();
            state.lastRepathTick = serverTick;
        }

        if (state.path.isEmpty()) return false;

        while (state.nextIndex < state.path.size() && isReached(player, state.path.get(state.nextIndex))) {
            state.nextIndex++;
        }
        if (state.nextIndex >= state.path.size()) {
            return true;
        }

        BlockPos next = state.path.get(state.nextIndex);
        moveTowards(player, next);
        return false;
    }

    private static boolean needsRepath(PathState state, BlockPos currentFeet, BlockPos goal, int serverTick) {
        if (state.goal == null || !state.goal.equals(goal)) return true;
        if (state.path.isEmpty()) return true;
        if (state.nextIndex >= state.path.size()) return true;
        if (serverTick - state.lastRepathTick >= REPATH_INTERVAL_TICKS) return true;

        BlockPos targetStep = state.path.get(Math.min(state.nextIndex, state.path.size() - 1));
        int dx = Math.abs(currentFeet.getX() - targetStep.getX());
        int dz = Math.abs(currentFeet.getZ() - targetStep.getZ());
        return dx > 4 || dz > 4;
    }

    private static boolean isReached(ServerPlayer player, BlockPos step) {
        double tx = step.getX() + 0.5;
        double tz = step.getZ() + 0.5;
        double dx = player.getX() - tx;
        double dz = player.getZ() - tz;
        double dy = Math.abs(player.getY() - step.getY());
        return (dx * dx + dz * dz) < 0.7 * 0.7 && dy < 1.25;
    }

    private static void moveTowards(ServerPlayer player, BlockPos next) {
        double tx = next.getX() + 0.5;
        double tz = next.getZ() + 0.5;
        double dx = tx - player.getX();
        double dz = tz - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.0e-4) return;

        double speed = 0.26;
        double vx = (dx / dist) * speed;
        double vz = (dz / dist) * speed;
        double vy = player.getDeltaMovement().y;

        int targetY = next.getY();
        if (player.onGround() && targetY > player.blockPosition().getY()) {
            vy = 0.42;
        }

        player.setDeltaMovement(vx, vy, vz);
        player.setSprinting(false);
        player.hurtMarked = true;
    }

    private static List<BlockPos> findPath(ServerLevel level, BlockPos startRaw, BlockPos goalRaw) {
        BlockPos start = snapWalkable(level, startRaw.getX(), startRaw.getY(), startRaw.getZ());
        BlockPos goal = snapWalkable(level, goalRaw.getX(), goalRaw.getY(), goalRaw.getZ());
        if (start == null || goal == null) return List.of();

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        Map<Long, Double> bestG = new HashMap<>();
        Map<Long, Long> parent = new HashMap<>();
        Map<Long, BlockPos> positions = new HashMap<>();

        long startKey = start.asLong();
        open.add(new Node(start, 0.0, heuristic(start, goal)));
        bestG.put(startKey, 0.0);
        positions.put(startKey, start);

        int expanded = 0;
        while (!open.isEmpty() && expanded < MAX_SEARCH_NODES) {
            Node node = open.poll();
            expanded++;

            long nodeKey = node.pos.asLong();
            double best = bestG.getOrDefault(nodeKey, Double.MAX_VALUE);
            if (node.g > best + 1.0e-6) continue;

            if (isGoal(node.pos, goal)) {
                return reconstructPath(parent, positions, nodeKey);
            }

            for (BlockPos next : neighbors(level, node.pos, start)) {
                long nextKey = next.asLong();
                double nextG = node.g + stepCost(node.pos, next);
                if (nextG + 1.0e-6 >= bestG.getOrDefault(nextKey, Double.MAX_VALUE)) continue;

                bestG.put(nextKey, nextG);
                parent.put(nextKey, nodeKey);
                positions.put(nextKey, next);
                open.add(new Node(next, nextG, heuristic(next, goal)));
            }
        }

        return List.of();
    }

    private static boolean isGoal(BlockPos current, BlockPos goal) {
        int dx = Math.abs(current.getX() - goal.getX());
        int dz = Math.abs(current.getZ() - goal.getZ());
        int dy = Math.abs(current.getY() - goal.getY());
        return dx <= 1 && dz <= 1 && dy <= 2;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        int dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz) + Math.abs(dy) * 0.4;
    }

    private static double stepCost(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dz = Math.abs(a.getZ() - b.getZ());
        int dy = Math.abs(a.getY() - b.getY());
        double base = (dx + dz == 2) ? 1.4 : 1.0;
        return base + dy * 0.3;
    }

    private static List<BlockPos> neighbors(ServerLevel level, BlockPos current, BlockPos start) {
        List<BlockPos> out = new ArrayList<>(8);
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                if (ox == 0 && oz == 0) continue;
                int nx = current.getX() + ox;
                int nz = current.getZ() + oz;

                if (Math.abs(nx - start.getX()) > MAX_RANGE_FROM_START
                        || Math.abs(nz - start.getZ()) > MAX_RANGE_FROM_START) {
                    continue;
                }

                BlockPos step = resolveStep(level, current, nx, nz);
                if (step != null) out.add(step);
            }
        }
        return out;
    }

    private static BlockPos resolveStep(ServerLevel level, BlockPos from, int nx, int nz) {
        int[] dy = {0, 1, -1, 2, -2};
        for (int d : dy) {
            int ny = from.getY() + d;
            BlockPos candidate = new BlockPos(nx, ny, nz);
            if (isWalkable(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static BlockPos snapWalkable(ServerLevel level, int x, int aroundY, int z) {
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4, -4};
        for (int off : offsets) {
            int y = aroundY + off;
            BlockPos p = new BlockPos(x, y, z);
            if (isWalkable(level, p)) return p;
        }
        return null;
    }

    private static boolean isWalkable(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= level.getMinBuildHeight() + 1) return false;
        if (feet.getY() >= level.getMaxBuildHeight() - 2) return false;

        BlockPos head = feet.above();
        BlockPos ground = feet.below();
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(head);
        BlockState groundState = level.getBlockState(ground);

        boolean feetFree = feetState.getCollisionShape(level, feet).isEmpty();
        boolean headFree = headState.getCollisionShape(level, head).isEmpty();
        boolean support = groundState.isFaceSturdy(level, ground, Direction.UP);
        return feetFree && headFree && support;
    }

    private static List<BlockPos> reconstructPath(Map<Long, Long> parent, Map<Long, BlockPos> positions, long endKey) {
        List<BlockPos> reversed = new ArrayList<>();
        long cursor = endKey;
        while (positions.containsKey(cursor)) {
            reversed.add(positions.get(cursor));
            Long p = parent.get(cursor);
            if (p == null) break;
            cursor = p;
        }

        List<BlockPos> path = new ArrayList<>(reversed.size());
        for (int i = reversed.size() - 1; i >= 0; i--) {
            path.add(reversed.get(i));
        }
        return path;
    }

    private static final class Node {
        private final BlockPos pos;
        private final double g;
        private final double h;

        private Node(BlockPos pos, double g, double h) {
            this.pos = pos;
            this.g = g;
            this.h = h;
        }

        private double f() {
            return g + h;
        }
    }

    private static final class PathState {
        private BlockPos goal;
        private List<BlockPos> path = List.of();
        private int nextIndex = 0;
        private int lastRepathTick = -99999;
    }
}
