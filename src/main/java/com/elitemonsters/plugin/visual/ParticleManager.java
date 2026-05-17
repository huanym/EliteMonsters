package com.elitemonsters.plugin.visual;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class ParticleManager {

    public static void drawCircle(Location center, Particle particle, double radius, int points, double yOffset) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            world.spawnParticle(particle, x, center.getY() + yOffset, z, 1, 0, 0, 0, 0);
        }
    }

    public static void drawHelix(Location center, Particle particle, double radius, double height, int points, int rotations) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            double angle = 2 * Math.PI * rotations * progress;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + progress * height;
            world.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    public static void drawBurst(Location center, Particle particle, double radius, int count, double yOffset) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            Vector dir = new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize();
            world.spawnParticle(particle, center.getX(), center.getY() + yOffset, center.getZ(), 0, dir.getX(), 0, dir.getZ(), 1);
        }
    }

    public static void drawSphere(Location center, Particle particle, double radius, int rings, int pointsPerRing) {
        World world = center.getWorld();
        if (world == null) return;
        for (int ring = 0; ring <= rings; ring++) {
            double phi = Math.PI * ring / rings;
            double currentRadius = radius * Math.sin(phi);
            double y = center.getY() + radius * Math.cos(phi);
            for (int i = 0; i < pointsPerRing; i++) {
                double theta = 2 * Math.PI * i / pointsPerRing;
                double x = center.getX() + currentRadius * Math.cos(theta);
                double z = center.getZ() + currentRadius * Math.sin(theta);
                world.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void drawLine(Location from, Location to, Particle particle, int points) {
        World world = from.getWorld();
        if (world == null) return;
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        direction.normalize();
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points * length;
            Location point = from.clone().add(direction.clone().multiply(t));
            world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }
}