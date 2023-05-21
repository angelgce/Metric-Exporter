package com.metrics.centos.service;

import java.util.List;

public interface ICommand {

    String getPrometheusMetrics();

    void loadIni();

    List<Metric> loadData();

    //getting the screen of each command
    String getCommandScreen(String command);

    //map each resul of each command
    String mapCommandsToPrometheus(Metric metric);

    String mapMetricValueResult(String result);

}
