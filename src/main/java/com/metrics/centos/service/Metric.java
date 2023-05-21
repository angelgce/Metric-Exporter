package com.metrics.centos.service;

import com.metrics.centos.exceptions.BadRequestException;
import lombok.Data;
import org.apache.commons.configuration2.INIConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class Metric {

    private final String[] PROPERTIES = {"command", "prom_query", "group_name", "group_label", "regex"};

    private INIConfiguration iniConfiguration;
    private String command, query, group, label, regex, screen;
    private List<String> labelList, groupList, values;
    private int index;
    private long regexGroupSize;

    public Metric(int index, INIConfiguration iniConfiguration) {
        this.index = index;
        this.iniConfiguration = iniConfiguration;
        try {
            this.command = iniConfiguration.getSection("command_" + index).getProperty(PROPERTIES[0]).toString();
            this.query = iniConfiguration.getSection("command_" + index).getProperty(PROPERTIES[1]).toString();
            this.group = iniConfiguration.getSection("command_" + index).getProperty(PROPERTIES[2]).toString();
            String mapLabel = iniConfiguration.getSection("command_" + index).getProperty(PROPERTIES[3]).toString();
            if (mapLabel == null || mapLabel.length() < 1) mapLabel = "";
            this.label = mapLabel;
            this.regex = iniConfiguration.getSection("command_" + index).getProperty(PROPERTIES[4]).toString();
            this.labelList = new ArrayList<>();
            this.groupList = new ArrayList<>();
            this.values = new ArrayList<>();

            validateFields();
        } catch (Exception ex) {
            System.out.println(ex.getCause());
            throw new BadRequestException("Error, mission information on ini file " + ex.getMessage());
        }
    }

    private void validateFields() {
        values.addAll(Arrays.asList(command, query, group, label, regex));
        //label cant be null (this case i == 3)
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == null && i != 3 || values.get(i).length() < 1 && i != 3)
                throw new BadRequestException
                        ("Error, value in property [" + PROPERTIES[i] + "] is null");
        }
        if (!group.contains(";"))
            throw new BadRequestException
                    ("group values must has had been separate by a semicolon ;");

        groupList.addAll(Arrays.asList(group.split(";")));

        if (!label.equals("")) labelList.addAll(Arrays.asList(label.split(";")));

        regexGroupSize = Arrays
                .asList(regex.split(""))
                .stream()
                .filter(item -> item.startsWith("("))
                .count();

        if (groupList.size() != regexGroupSize)
            throw new BadRequestException(
                    "There is " + groupList.size()
                            + " groups to collect " + groupList
                            + " but there's only " + regexGroupSize
                            + " groups on regex " + regex);
    }
}
