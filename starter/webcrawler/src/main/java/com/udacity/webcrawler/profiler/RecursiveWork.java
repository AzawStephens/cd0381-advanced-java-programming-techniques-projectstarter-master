package com.udacity.webcrawler.profiler;



import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class RecursiveWork extends RecursiveAction {
    private String url;
    private Instant deadline;
    private int maxDepth;
    private Map<String, Integer> counts = Collections.synchronizedMap(new HashMap<>());
    private ConcurrentSkipListSet<String> visitedUrls;
    private Clock clock;
    private List<Pattern> ignoredUrls;
    private PageParserFactory parserFactory;

    public RecursiveWork(String url, Instant deadline, int maxDepth, Map<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls) {
        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;

        InternalCrawlerBuilder internalCrawlerBuilder = new InternalCrawlerBuilder.Builder()
                .setUrl(url)
                .setDeadLine(deadline)
                .setMaxDepth(maxDepth)
                .setCounts(counts)
                .setVisitedUrls(visitedUrls)
                .build();
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return; //stop crawling after a certain amount of time
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return; //stops ignored urls from being added
            }
        }
        if (visitedUrls.contains(url)) {
            return; //stops duplicate urls from being added
        }
        visitedUrls.add(url); //If everything is good, add the url to visited urls set
        PageParser.Result result = parserFactory.get(url).parse(); //parses the url to the PageParser
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) { //Get the entry set for every word count
            counts.compute(e.getKey(), (K, V) -> (V == null) ? e.getValue() : e.getValue() + V);// the counts collection contains the entry set, put the entry into the count collection and get the entry set
        }
        List<RecursiveWork> subtasks = new ArrayList<>();
        for (String aUrl : result.getLinks()) {
            subtasks.add(new RecursiveWork(aUrl, deadline, maxDepth - 1, counts, visitedUrls));
        }
        invokeAll(subtasks);
    }
}