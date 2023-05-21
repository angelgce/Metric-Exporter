package com.metrics.centos.service;

import com.metrics.centos.exceptions.BadRequestException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.INIConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Data
@Slf4j

public class ServiceRemoteCommand implements ICommand {

    private final Environment environment;


    @Value("${app.config.file}")
    private String path;
    private INIConfiguration iniConfiguration;

    @Override
    public String getPrometheusMetrics() {
        loadIni();
        List<Metric> metrics = loadData();
        StringBuilder output = new StringBuilder();
        metrics.forEach(metric -> {
            String result = mapCommandsToPrometheus(metric);
            output.append(result);
        });


        return output.toString();
    }

    @Override
    public void loadIni() {
        try {
            iniConfiguration = new INIConfiguration();
            FileReader fileReader = new FileReader(path);
            iniConfiguration.read(fileReader);
        } catch (Exception e) {
            throw new BadRequestException("File not founded ".concat(path));
        }
    }

    @Override
    public List<Metric> loadData() {
        List<Metric> metrics = new ArrayList<>();
        String value = "";
        int size = 0;
        try {
            value = iniConfiguration
                    .getSection("config")
                    .getProperty("total_commands")
                    .toString();

            size = Integer.valueOf(value);
        } catch (Exception e) {
            throw new BadRequestException
                    (value + " Cannot be casted as number try to modify the value of total_commands in [config] section  ");
        }
        for (int i = 0; i < size; i++) {
            if (iniConfiguration.getSection("command_" + (i + 1)).isEmpty()) {
                throw new BadRequestException
                        ("total_commands in section [config] is " + size + " but you only have " + i + " command_ sections");
            }
            //creating a metric object
            Metric metric = new Metric((i + 1), iniConfiguration);
            String screen = getCommandScreen(metric.getCommand());
            metric.setScreen(screen);
            metrics.add(metric);
        }
        return metrics;
    }

    @Override
    public String getCommandScreen(String command) {
        StringBuilder output = new StringBuilder();
        String string;
        Process process;
        try {
            process = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((string = br.readLine()) != null)
                output.append(string + "\n");
            process.waitFor();
            process.exitValue();
            process.destroy();
        } catch (Exception e) {
        }
        if (output.isEmpty()) throw new BadRequestException("Command [".concat(command).concat("] has not data"));
        return output.toString();

    }

    @Override
    public String mapCommandsToPrometheus(Metric metric) {
        //Map to order the metrics in group
        HashMap<String, List<String>> orderResult = new HashMap<>();
        StringBuilder builder = new StringBuilder();

        //Getting all the lines of the screen
        List<String> lines = Arrays.asList(metric.getScreen().split("\n"));

        //mapping information per line
        lines.forEach(line -> {
            //Getting the data from regex and groups
            List<Data> results = new ArrayList<>();
            Pattern pattern = Pattern.compile(metric.getRegex().trim());
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                for (int j = 1; j <= metric.getRegexGroupSize(); j++) {
                    String groupName = metric.getGroupList().get(j - 1);
                    String result = matcher.group(j);
                    results.add(new Data(groupName, result.trim()));
                }
            }
            //if a line not match with the regex, inform in logs
            if (results.isEmpty()) {
                log.warn("\nError: Not Mach \nLine: {} \nRegex: {} \n", line, metric.getRegex());
            }
            //mapping the information to the prometheus format
            for (int i = 0; i < results.size(); i++) {
                String groupName = results.get(i).name;

                //labels will be inside  the metric { here="value"} and won't be exposed as metric
                boolean isLabel = metric.getLabelList()
                        .stream()
                        .anyMatch(label -> label.equals(groupName));

                if (!isLabel) {
                    StringBuilder lineInPromFormat = new StringBuilder();
                    String key = metric
                            .getGroupList()
                            .get(i)
                            .replaceAll("[\\W]", "")
                            .toLowerCase();

                    //will save the metrics order by groups names
                    if (!orderResult.containsKey(key))
                        orderResult.put(key, new ArrayList<>());

                    //starting the format metric_example{
                    lineInPromFormat
                            .append(metric.getQuery())
                            .append("_")
                            .append(key);
                    // body of the query {label1="value1", label2="value2"
                    if (metric.getLabelList().size() > 0) lineInPromFormat.append("{");
                    for (int j = 0; j < metric.getLabelList().size(); j++) {
                        String item = metric.getLabelList().get(j);
                        Optional<Data> labelResult = results
                                .stream()
                                .filter(filter -> filter.name.equals(item))
                                .findAny();
                        lineInPromFormat
                                .append(labelResult.get().name.toLowerCase())
                                .append("=\"")
                                .append(labelResult.get().value)
                                .append("\"");
                        if (metric.getLabelList().size() > 1 && j < (metric.getLabelList().size() - 1))
                            lineInPromFormat.append(",");
                    }
                    //} result
                    if (metric.getLabelList().size() > 0) lineInPromFormat.append("}");
                    lineInPromFormat.append(" ").append(mapMetricValueResult(results.get(i).value));
                    orderResult.get(key).add(lineInPromFormat.toString());
                }
            }
        });

        //ordering metrics by group names
        orderResult.forEach((key, value) -> {
            String text = "";
            if (!text.equals(key)) {
                String[] query = value.get(0).split("[{]");
                builder.append("# HELP ")
                        .append(query[0])
                        .append(" The value of ")
                        .append(key)
                        .append(".\n");
                builder.append("# TYPE ")
                        .append(query[0])
                        .append(" gauge\n");
                text = key;
            }
            value.forEach(item -> {
                builder.append(item)
                        .append("\n");
            });
        });
        return builder.toString();
    }

    @Override
    public String mapMetricValueResult(String result) {
        if (result.matches("[0-9]+[K]")) result = result.replaceAll("K", "");
        String[] prefix = {"M", "G", "T"};
        double[] values = {1024, 1048576, 1073741824};
        for (int i = 0; i < prefix.length; i++) {
            if (result.matches("[0-9]+".concat("[" + prefix[i] + "]"))) {
                double convert = Integer.valueOf(result.replaceAll("[" + prefix[i] + "]", "").trim());
                convert *= 1024.0;
                return String.valueOf(convert);
            }
        }
        return result;
    }

    public record Data(String name, String value) {
    }

}
