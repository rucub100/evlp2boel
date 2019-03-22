/*
 * Copyright (C) 2019 Heinrich-Heine-Universitaet Duesseldorf, Institute of Computer Science, Department Operating Systems
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package de.hhu.bsinfo.evlp2boel;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ruslan Curbanov, ruslan.curbanov@uni-duesseldorf.de, 22.03.2019
 *
 */
public final class EvlpToBoelConverter {
    private final Map<Long, List<Long>> edges;

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 4 || args.length > 5) {
            printHelp();
            System.exit(1);
        }

        int a = 0;
        boolean directed = false;

        if (args[0].contentEquals("-d")) {
            a++;
            directed = true;
        }

        final String path = args[a++];
        final String graph = args[a++];
        final int vertices = Integer.parseInt(args[a++]);

        System.out.println("Initialize converter...");
        final EvlpToBoelConverter converter = new EvlpToBoelConverter(vertices);
        System.out.println("Parse vertices...");
        parseVertices(path + File.separator + graph + ".v", converter.edges);
        System.gc();
        System.out.println("Parse edges...");
        parseEdges(path + File.separator + graph + ".e", directed, converter.edges);
        System.gc();
        System.out.println("Create binary file...");
        converter.convert(path + File.separator + graph + ".boel");
        System.out.println(">>>SUCCESS<<<");
    }

    private EvlpToBoelConverter(final int vertices) {
        this.edges = new HashMap<Long, List<Long>>(vertices);
    }

    private final void convert(String path) {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(
                Paths.get(path),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE), 1000000))) {
            long cntVerticesTotal = edges.keySet().size();
            long cntVerticesWithoutNeighbors = 0;
            long cnt = 0;
            final long cntMod = 1000;
            int p = 0;

            for (long vid : edges.keySet()) {
                
                int cntNeighbors = edges.get(vid).size();
                // write long (vid)
                // write int (cntNeighbors)
                dos.writeLong(vid);
                dos.writeInt(cntNeighbors);

                if (cntNeighbors == 0) {
                    cntVerticesWithoutNeighbors++;
                }

                for (int i = 0; i < cntNeighbors; i++) {
                    dos.writeLong(edges.get(vid).get(i));
                }

                cnt++;
                if (cnt % cntMod == 0) {
                    int tmp = (int) ((100.0 * cnt) / cntVerticesTotal);
                    if (tmp > p) {
                        p = tmp;
                        System.out.println(String.format("Processing: %d%% finished...", p));
                    }
                }
            }

            int tmp = (int) ((100.0 * cntVerticesWithoutNeighbors) / cntVerticesTotal);
            System.out.println(String.format("Disconnected vertices:  %d%%...", tmp));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseEdges(final String path, final boolean directed, final Map<Long, List<Long>> edges) {
        try (final BufferedReader br = Files.newBufferedReader(
                Paths.get(path),
                StandardCharsets.US_ASCII)) {
            String line = null;
            final int outMod = 1000000;
            long cntEdges = 0;
            while ((line = br.readLine()) != null) {
                long left = Long.parseLong(line.split("\\s")[0]);
                long right = Long.parseLong(line.split("\\s")[1]);

                edges.get(left).add(right);

                if (!directed) {
                    edges.get(left).add(right);
                }

                cntEdges++;
                if (cntEdges % outMod == 0) {
                    System.out.println(String.format("Processing: %dM edges finished...", (cntEdges / outMod)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseVertices(final String path, final Map<Long, List<Long>> edges) {
        try (final BufferedReader br = Files.newBufferedReader(
                Paths.get(path),
                StandardCharsets.US_ASCII)) {
            String line = null;
            final int outMod = 1000000;
            long cntVertices = 0;
            while ((line = br.readLine()) != null) {
                long vid = Long.parseLong(line.split("\\s")[0]);

                if (!edges.containsKey(vid)) {
                    edges.put(vid, new ArrayList<Long>());
                }

                cntVertices++;
                if (cntVertices % outMod == 0) {
                    System.out.println(String.format("Processing: %dM vertices finished...", (cntVertices / outMod)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printHelp() {
        System.out.println("Convert EVLP (Edge/Vertex-List with Properties) ASCII text files to a binary ordered edge list.\n");
        System.out.println("usage: evlp2boel [-d] <path/to/graph> <graph-name> <#vertices> <output-file>\n");
        System.out.println("    -d  Directed graph");
    }
}
