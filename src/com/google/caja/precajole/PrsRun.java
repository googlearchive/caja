package com.google.caja.precajole;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParserContext;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.JobEnvelope;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.stages.PrecajoleRewriteStage;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Pipeline.Stage;

import java.net.URI;

public class PrsRun {

  private static final String HTML =
      "<script src=\"http://code.jquery.com/jquery-1.6.4.js\"></script>"
      +"<force-messages></force-messages>";

  public static void main(String args[]) throws Exception{
    int n = Integer.valueOf(args[0]);
    //run();
    long t0 = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      run();
    }
    long t1 = System.currentTimeMillis();
    System.out.println(t1 - t0);
    System.out.println((double)(t1 - t0) / n);
  }

  private static void run() throws Exception {
    MessageQueue mq = new SimpleMessageQueue();
    MessageContext mc = new MessageContext();
    PluginMeta meta = new PluginMeta();
    ParseTreeNode root = (
        new ParserContext(mq)
        .withInput(HTML)
        .withConfig(meta)
        .withConfig(mc)
        .build());
    Jobs jobs = new Jobs(mc, mq, meta);
    jobs.getJobs().add(JobEnvelope.of(Job.job(root, new URI("http://boo/"))));
    Stage<Jobs> st = new PrecajoleRewriteStage(
        meta.getPrecajoleMap(), meta.getPrecajoleMinify());
    st.apply(jobs);
  }
}
