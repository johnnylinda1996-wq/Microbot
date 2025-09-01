package net.runelite.client.plugins.microbot.jpnl.accountbuilder.queue;

import net.runelite.client.plugins.microbot.jpnl.accountbuilder.tasks.AioTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class QueueManager {
    private final List<AioTask> queue = new CopyOnWriteArrayList<>();

    public void add(AioTask t) { queue.add(t); }
    public AioTask poll() { return queue.isEmpty() ? null : queue.remove(0); }
    public void remove(int index) { if (index >=0 && index < queue.size()) queue.remove(index); }
    public void clear() { queue.clear(); }
    public List<AioTask> snapshot() { return new ArrayList<>(queue); }

    public void setNewOrder(List<AioTask> ordered) {
        queue.clear();
        queue.addAll(ordered);
    }

    public int size() { return queue.size(); }
}