package com.udacity.webcrawler.profiler;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public class RecursiveWorks  extends RecursiveTask<Boolean> {
    String url;
    Instant deadLIne;
    int maxDepth;
    Map<String, Integer> counts;
    Set<String> visitedUrls;
    public RecursiveWorks(String url, Instant deadLIne, int maxDepth, Map<String, Integer> counts, Set<String> visitedUrls)
    {
        this.url = url;
        this.deadLIne = deadLIne;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        InternalCrawlerBuilder internalCrawlerBuilder = new InternalCrawlerBuilder.Builder()
                .setUrl(url)
                .setDeadLine(deadLIne)
                .setMaxDepth(maxDepth)
                .setCounts(counts)
                .setVisitedUrls(visitedUrls)
                .build();
    }
    @Override
    protected Boolean compute() {
        return false;
    }
}
