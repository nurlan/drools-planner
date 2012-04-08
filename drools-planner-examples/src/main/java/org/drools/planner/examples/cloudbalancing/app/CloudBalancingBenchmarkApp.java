/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.planner.examples.cloudbalancing.app;

import org.drools.planner.examples.common.app.CommonBenchmarkApp;

public class CloudBalancingBenchmarkApp extends CommonBenchmarkApp {

    public static final String DEFAULT_BENCHMARK_CONFIG
            = "/org/drools/planner/examples/cloudbalancing/benchmark/cloudBalancingBenchmarkConfig.xml";
    public static final String SCORE_DIRECTOR_BENCHMARK_CONFIG
            = "/org/drools/planner/examples/cloudbalancing/benchmark/cloudBalancingScoreDirectorBenchmarkConfig.xml";

    public static void main(String[] args) {
        String benchmarkConfig;
        if (args.length > 0) {
            if (args[0].equals("default")) {
                benchmarkConfig = DEFAULT_BENCHMARK_CONFIG;
            } else if (args[0].equals("scoreDirector")) {
                benchmarkConfig = SCORE_DIRECTOR_BENCHMARK_CONFIG;
            } else {
                throw new IllegalArgumentException("The program argument (" + args[0] + ") is not supported.");
            }
        } else {
            benchmarkConfig = DEFAULT_BENCHMARK_CONFIG;
        }
        new CloudBalancingBenchmarkApp(benchmarkConfig).process();
    }

    public CloudBalancingBenchmarkApp(String benchmarkConfig) {
        super(benchmarkConfig);
    }

}
