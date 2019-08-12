package com.adaptris.kubernetes.metrics.prometheus;

import com.adaptris.core.ComponentLifecycleExtension;

public interface MessageMetricsCollector extends Runnable, MessageMetricsNotifier, ComponentLifecycleExtension {

}
