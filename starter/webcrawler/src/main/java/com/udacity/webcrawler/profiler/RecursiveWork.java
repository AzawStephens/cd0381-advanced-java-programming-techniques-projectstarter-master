package com.udacity.webcrawler.profiler;



import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public class RecursiveWork extends RecursiveTask<Boolean> {
    String url;
    Instant deadline;
    int maxDepth;
    ConcurrentMap<String, Integer> counts;
    ConcurrentSkipListSet<String> visitedUrls;// may have to change this to ConcorrentSkipListSet(Set<String>) later
    Clock clock;
    List<Pattern> ignoredUrls;
    PageParserFactory parserFactory;

    public RecursiveWork(String url, Instant deadline, int maxDepth, ConcurrentMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls, List<Pattern> ignoredUrls, Clock clock, PageParserFactory parserFactory) {
        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
        this.clock = clock;
        this.parserFactory = parserFactory;
        InternalCrawlerBuilder internalCrawlerBuilder = new InternalCrawlerBuilder.Builder()
                .setUrl(url)
                .setDeadLine(deadline)
                .setMaxDepth(maxDepth)
                .setCounts(counts)
                .setVisitedUrls(visitedUrls)
                .setIgnoredUrls(ignoredUrls)
                .setClock(clock)
                .setParserFactory(parserFactory)
                .build();
    }

    @Override
    protected Boolean compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return false; //stop crawling after a certain amount of time
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return false; //stops ignored urls from being added
            }
        }
        if (visitedUrls.contains(url)) {
            return false; //stops duplicate urls from being added
        }

        visitedUrls.add(url); //If everything is good, add the url to visited urls set
        PageParser.Result result = parserFactory.get(url).parse(); //parses the url to the PageParser
        for (ConcurrentMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) { //Get the entry set for every word count
            counts.compute(e.getKey(), (K, V) -> (V == null) ? e.getValue() : e.getValue() + counts.get(e.getKey()));// the counts collection contains the entry set, put the entry into the count collection and get the entry set
        }
        List<RecursiveWork> subtasks = new ArrayList<>();
        for (String aUrl : result.getLinks()) {
            subtasks.add(new RecursiveWork(aUrl, deadline, maxDepth - 1, counts, visitedUrls, ignoredUrls, clock, parserFactory));
        }
        invokeAll(subtasks);
        return true;
    }
}
