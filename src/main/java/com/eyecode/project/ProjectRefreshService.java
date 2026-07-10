package com.eyecode.project;

import com.eyecode.editor.v2.project.ProjectIndexer;
import com.eyecode.editor.v2.project.ProjectSymbolIndex;
import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.SubscriptionToken;
import com.eyecode.eventbus.events.ProjectRefreshEvent;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinates incremental refresh of the project's derived state.
 * <p>
 * Listens to {@link ProjectRefreshEvent} on the {@link EventBus} and applies
 * only the affected update:
 * <ul>
 *   <li>{@link ProjectRefreshEvent.Kind#FILE_CREATED} → index file</li>
 *   <li>{@link ProjectRefreshEvent.Kind#FILE_MODIFIED} → update file symbols</li>
 *   <li>{@link ProjectRefreshEvent.Kind#FILE_DELETED} → remove file symbols</li>
 *   <li>{@link ProjectRefreshEvent.Kind#FILE_RENAMED} → remove old path and index new path</li>
 *   <li>{@link ProjectRefreshEvent.Kind#DIRECTORY_CREATED}/{@code DIRECTORY_DELETED} → no index change</li>
 * </ul>
 * Tree listeners are notified after the symbol index is updated so the explorer
 * can patch only the affected node, never rebuild the whole tree.
 */
public final class ProjectRefreshService {

    public interface TreeChangeListener {
        void onRefresh(ProjectRefreshEvent event);
    }

    private final EventBus eventBus;
    private ProjectIndexer indexer;
    private final ProjectSymbolIndex index;
    private final List<TreeChangeListener> treeListeners = new CopyOnWriteArrayList<>();
    private SubscriptionToken subscriptionToken;

    public ProjectRefreshService(EventBus eventBus, ProjectIndexer indexer, ProjectSymbolIndex index) {
        this.eventBus = eventBus;
        this.indexer = indexer;
        this.index = index;
    }

    public void setIndexer(ProjectIndexer indexer) {
        this.indexer = indexer;
    }

    public void start() {
        if (subscriptionToken != null) return;
        subscriptionToken = eventBus.subscribe(ProjectRefreshEvent.class, this::handle);
    }

    public void stop() {
        if (subscriptionToken != null) {
            eventBus.unsubscribe(subscriptionToken);
            subscriptionToken = null;
        }
    }

    public void addTreeChangeListener(TreeChangeListener listener) {
        if (listener != null) treeListeners.add(listener);
    }

    public void removeTreeChangeListener(TreeChangeListener listener) {
        treeListeners.remove(listener);
    }

    public void emit(ProjectRefreshEvent event) {
        if (event == null) return;
        eventBus.publish(event);
    }

    private void handle(ProjectRefreshEvent event) {
        if (event == null) return;
        applyToIndex(event);
        notifyTreeListeners(event);
    }

    private void applyToIndex(ProjectRefreshEvent event) {
        if (indexer == null || index == null) return;
        Path path = event.getPath();
        switch (event.getKind()) {
            case FILE_CREATED -> indexer.indexFile(path, index);
            case FILE_MODIFIED -> indexer.updateFile(path, index);
            case FILE_DELETED -> indexer.removeFile(path, index);
            case FILE_RENAMED -> {
                if (event.hasOldPath()) {
                    indexer.removeFile(event.getOldPath(), index);
                }
                indexer.indexFile(path, index);
            }
            case DIRECTORY_CREATED, DIRECTORY_DELETED -> { /* no symbol change */ }
        }
    }

    private void notifyTreeListeners(ProjectRefreshEvent event) {
        for (TreeChangeListener listener : treeListeners) {
            listener.onRefresh(event);
        }
    }
}
