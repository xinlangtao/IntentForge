package cn.intentforge.boot.server;

import cn.intentforge.agent.core.AgentRunEvent;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

final class AgentRunEventBroker {
  private final ConcurrentMap<String, CopyOnWriteArrayList<BlockingQueue<AgentRunEvent>>> queuesByRunId = new ConcurrentHashMap<>();

  void publish(AgentRunEvent event) {
    List<BlockingQueue<AgentRunEvent>> queues = queuesByRunId.get(event.runId());
    if (queues == null) {
      return;
    }
    for (BlockingQueue<AgentRunEvent> queue : queues) {
      queue.offer(event);
    }
  }

  Subscription subscribe(String runId) {
    BlockingQueue<AgentRunEvent> queue = new LinkedBlockingQueue<>();
    queuesByRunId.computeIfAbsent(runId, ignored -> new CopyOnWriteArrayList<>()).add(queue);
    return new Subscription(runId, queue);
  }

  final class Subscription implements AutoCloseable {
    private final String runId;
    private final BlockingQueue<AgentRunEvent> queue;

    private Subscription(String runId, BlockingQueue<AgentRunEvent> queue) {
      this.runId = runId;
      this.queue = queue;
    }

    BlockingQueue<AgentRunEvent> queue() {
      return queue;
    }

    @Override
    public void close() {
      List<BlockingQueue<AgentRunEvent>> queues = queuesByRunId.get(runId);
      if (queues == null) {
        return;
      }
      queues.remove(queue);
      if (queues.isEmpty()) {
        queuesByRunId.remove(runId, queues);
      }
    }
  }
}
