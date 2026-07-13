package br.commons.framework.logger.bridge;

import org.jspecify.annotations.NullMarked;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@NullMarked
public final class SLF4JServiceProvider implements org.slf4j.spi.SLF4JServiceProvider, ILoggerFactory {

    // ###################################################
    // SLF4JServiceProvider
    // ###################################################
    private IMarkerFactory markerFactory = new BasicMarkerFactory();
    private MDCAdapter mdcAdapter = new NOPMDCAdapter();

    @Override
    public void initialize() {
        // Já inicializados na declaração para segurança de nulos
    }

    @Override public ILoggerFactory getLoggerFactory() { return this; }
    @Override public IMarkerFactory getMarkerFactory() { return markerFactory; }
    @Override public MDCAdapter getMDCAdapter() { return mdcAdapter; }
    @Override public String getRequestedApiVersion() { return "2.0.17"; }
    // ###################################################
    // ILoggerFactory
    // ###################################################
    private static final Map<String, Logger> loggers = new ConcurrentSkipListMap<>();
    @Override public Logger getLogger(String name) { return loggers.computeIfAbsent(name, SLF4JBridge::new); }

}
