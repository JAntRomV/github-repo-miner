package com.miner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Analiza pom.xml con un parser XML real (DOM) — javax.xml es parte del JDK,
 * no requiere dependencia nueva en el pom.xml.
 * Maven es declarativo: framework, Java 21 y GraalVM se leen directamente
 * de <parent>, <properties>, <dependencies> y <plugins>.
 */
public class MavenAnalyzer implements BuildFileAnalyzer {

    // Solo cuenta *Test.java DENTRO de src/test — más preciso que buscar en todo el árbol
    private static final Pattern TEST_FILE_PATTERN = Pattern.compile("^src/test/.*Test\\.java$");

    @Override
    public boolean supports(BuildTool tool) {
        return tool == BuildTool.MAVEN;
    }

    @Override
    public TechProfile analyze(FileTree tree, Map<String, String> buildFiles) {
        String pomContent = buildFiles.get("pom.xml");
        if (pomContent == null) {
            return TechProfile.empty(BuildTool.MAVEN);
        }

        try {
            Document doc = parseXml(pomContent);
            Map<String, String> properties = extractProperties(doc);
            String parentArtifact = extractParentArtifactId(doc);
            List<String> dependencies = extractDependencies(doc);
            List<String> plugins = extractPlugins(doc);

            Framework framework = detectFramework(parentArtifact, dependencies);

            int javaVersion = detectJavaVersion(properties);
            boolean java21 = javaVersion == 21;

            // GraalVM: native-maven-plugin O directorio META-INF/native-image
            // O Micronaut, que es nativo por diseño (DI en tiempo de compilación, AOT)
            boolean graalvmReady = plugins.stream().anyMatch(p -> p.contains("native-maven-plugin"))
                || tree.pathContains("META-INF/native-image")
                || framework == Framework.MICRONAUT;

            boolean hasTestDir = tree.directoryExists("src/test/java");
            TestFramework testFramework = detectTestFramework(dependencies);
            boolean hasTestSuite = hasTestDir && testFramework != TestFramework.NONE;
            int testFileCount = tree.countMatching(TEST_FILE_PATTERN);

            boolean jmhPresent = tree.directoryExists("src/jmh")
                || dependencies.stream().anyMatch(d -> d.contains("jmh-core"));
            boolean jmhCandidate = jmhPresent || looksLikeJmhCandidate(tree);

            boolean profilingCandidate = tree.getPaths().stream()
                    .anyMatch(p -> p.endsWith("Application.java"))
                || dependencies.stream().anyMatch(d -> d.contains("micrometer"));

            // Sector parcial — el resto (topics de Fase 2) se refina en TechnicalFilterService
            Sector sector = tree.fileExists("CITATION.cff") ? Sector.ACADEMIC : Sector.UNKNOWN;

            boolean travisCi = tree.fileExists(".travis.yml");

            // FILTROS DUROS: framework correcto + Java 21 + set de pruebas
            boolean passesHardFilters = framework != Framework.NONE && java21 && hasTestSuite;

            return new TechProfile(
                BuildTool.MAVEN, framework, javaVersion, java21, graalvmReady,
                hasTestSuite, testFramework, testFileCount,
                jmhPresent, jmhCandidate, profilingCandidate,
                sector, travisCi, passesHardFilters
            );

        } catch (Exception e) {
            System.err.println("  [ERROR] Parseando pom.xml: " + e.getMessage());
            return TechProfile.empty(BuildTool.MAVEN);
        }
    }

    // ═══════════════════════════════════════════
    // Parseo XML con DOM (protegido contra XXE)
    // ═══════════════════════════════════════════
    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private Map<String, String> extractProperties(Document doc) {
        Map<String, String> props = new HashMap<>();
        NodeList propertiesNodes = doc.getElementsByTagName("properties");
        if (propertiesNodes.getLength() > 0) {
            NodeList children = propertiesNodes.item(0).getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    props.put(n.getNodeName(), n.getTextContent().trim());
                }
            }
        }
        return props;
    }

    private String extractParentArtifactId(Document doc) {
        NodeList parents = doc.getElementsByTagName("parent");
        if (parents.getLength() == 0) return "";
        Element parent = (Element) parents.item(0);
        NodeList artifactIds = parent.getElementsByTagName("artifactId");
        return artifactIds.getLength() > 0 ? artifactIds.item(0).getTextContent().trim() : "";
    }

    private List<String> extractDependencies(Document doc) {
        List<String> deps = new ArrayList<>();
        NodeList depNodes = doc.getElementsByTagName("dependency");
        for (int i = 0; i < depNodes.getLength(); i++) {
            Element dep = (Element) depNodes.item(i);
            deps.add(getChildText(dep, "groupId") + ":" + getChildText(dep, "artifactId"));
        }
        return deps;
    }

    private List<String> extractPlugins(Document doc) {
        List<String> plugins = new ArrayList<>();
        NodeList pluginNodes = doc.getElementsByTagName("plugin");
        for (int i = 0; i < pluginNodes.getLength(); i++) {
            Element plugin = (Element) pluginNodes.item(i);
            plugins.add(getChildText(plugin, "groupId") + ":" + getChildText(plugin, "artifactId"));
        }
        return plugins;
    }

    private String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : "";
    }

    // ═══════════════════════════════════════════
    // Detección de criterios
    // ═══════════════════════════════════════════
    private Framework detectFramework(String parentArtifact, List<String> dependencies) {
        boolean isSpringBoot = parentArtifact.contains("spring-boot-starter-parent")
            || dependencies.stream().anyMatch(d -> d.startsWith("org.springframework.boot:"));
        boolean isMicronaut = parentArtifact.contains("micronaut-parent")
            || dependencies.stream().anyMatch(d -> d.startsWith("io.micronaut:"));

        if (isSpringBoot && isMicronaut) return Framework.BOTH;
        if (isSpringBoot) return Framework.SPRING_BOOT;
        if (isMicronaut) return Framework.MICRONAUT;
        return Framework.NONE;
    }

    private int detectJavaVersion(Map<String, String> properties) {
        String raw = properties.getOrDefault("java.version",
                     properties.getOrDefault("maven.compiler.release", ""));

        // Resuelve UN nivel de placeholder: ${otra.propiedad}
        // LIMITACIÓN CONOCIDA: si la propiedad se hereda de un BOM padre externo, no se resuelve
        if (raw.startsWith("${") && raw.endsWith("}")) {
            raw = properties.getOrDefault(raw.substring(2, raw.length() - 1), "");
        }

        try {
            return raw.isEmpty() ? 0 : Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private TestFramework detectTestFramework(List<String> dependencies) {
        if (dependencies.stream().anyMatch(d -> d.contains("micronaut-test-junit5"))) {
            return TestFramework.MICRONAUT_TEST_JUNIT5;
        }
        if (dependencies.stream().anyMatch(d -> d.contains("spring-boot-starter-test"))) {
            return TestFramework.SPRING_BOOT_STARTER_TEST;
        }
        if (dependencies.stream().anyMatch(d -> d.contains("junit-jupiter"))) {
            return TestFramework.JUNIT_JUPITER;
        }
        return TestFramework.NONE;
    }

    // Heurística de menor confianza: módulos CPU-bound típicos (códecs, parsers, serialización)
    private boolean looksLikeJmhCandidate(FileTree tree) {
        return tree.getPaths().stream().anyMatch(p ->
            p.contains("codec") || p.contains("parser") ||
            p.contains("serializ") || p.contains("algorithm"));
    }
}