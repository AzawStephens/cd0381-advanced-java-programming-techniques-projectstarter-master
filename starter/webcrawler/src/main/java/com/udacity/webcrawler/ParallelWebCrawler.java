package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;
import com.udacity.webcrawler.profiler.WebCrawlerTasks;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler
{
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final PageParserFactory parserFactory;
  private final List<Pattern> ignoredUrls;

  @Inject
  ParallelWebCrawler(
      Clock clock,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @TargetParallelism int threadCount,
      PageParserFactory parserFactory,
      @IgnoredUrls List<Pattern>  ignoredUrls) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.parserFactory = parserFactory;
    this.ignoredUrls = ignoredUrls;
  }


  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    Map<String, Integer> counts = new HashMap<>();
    Set<String> visitedUrls = new HashSet<>();
    for(String url: startingUrls)
    {
      PageParser.Result result = parserFactory.get(url).parse();
      //perform the Internal crawl here.
      if(getMaxParallelism() > 1)
      {
        //this is where the forkjoinpool would take place.
      }
    }
    if (counts.isEmpty()) {
      return new CrawlResult.Builder()
              .setWordCounts(counts)//if a word was not found in the crawl, set word count to what is inside of the counts Map
              .setUrlsVisited(visitedUrls.size()) // set the set of urls visited to the size of urls that were visited during the crawl
              .build();  //Lastly, build the Crawl result
    }

    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(counts, popularWordCount)) //If the crawl did find words, add them to the CrawlResult, sorting them by popular words
            .setUrlsVisited(visitedUrls.size())//set the amount of urls that were visited
            .build();//Lastly, build the CrawlResult
  }
  //Below is where the magic takes place.
  private void crawlInternal(
          String url,
          Instant deadline,
          int maxDepth,
          Map<String, Integer> counts,
          Set<String> visitedUrls) {
    if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
      return; //stop crawling after a certain amount of time
    }
    for (Pattern pattern : ignoredUrls) {
      if (pattern.matcher(url).matches()) {
        return; //stops ignored urls from being added

      }
      if (visitedUrls.contains(url)) {
        return; //stops duplicate urls from being added
      }
      visitedUrls.add(url); //If everything is good, add the url to visited urls set
      PageParser.Result result = parserFactory.get(url).parse(); //parses the url to the PageParser
      for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) { //Get the entry set for every word count
        if (counts.containsKey(e.getKey())) {
          counts.put(e.getKey(), e.getValue() + counts.get(e.getKey())); //if the counts collection contains the entry set, put the entry into the count collection and get the entry set
        } else {
          counts.put(e.getKey(), e.getValue()); //if the counts collection does not conatin the entry set put the entry into the counts
        }
      }
      for (String link : result.getLinks()) {
        crawlInternal(link, deadline, maxDepth - 1, counts, visitedUrls);
      }
    }
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
