/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package die.cafe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class App {

    public String getGreeting() {
        return "Hello world.";
    }

    public static void main(String[] args) {
        String dirName = ".";

        ClassFinder finder = new ClassFinder();

        List<String> classStrings;

        try {
            classStrings = finder.classFinder(dirName);
        } catch (IOException e) {
            e.printStackTrace();
            classStrings = List.of();
        }
        List<Class> classes = new ArrayList<>();

        for (String className : classStrings) {
            try {
                classes.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        HashMap<Class, Class[]> dependencyTree = new HashMap<>();
        for (Class clazz : classes) {
            dependencyTree.put(clazz, clazz.getConstructors()[0].getParameterTypes());
        }
        DependencyTree internalTree = new DependencyTree();
        System.out.println(classes);
        // Resolve Dependency Classes
        internalTree.resolve(dependencyTree);
        System.out.println("did it work?");
        Map<Class, Object> objects = new HashMap<>();
        Shell shell = new Shell();
        for (Class clazzy : internalTree.getInternalMap().keySet()) {
            try {
                objects.put(clazzy, internalTree.getInternalMap().get(clazzy).initialiseInstance(shell));
            } catch (Exception ex) {
                System.out.println("lol");
            }
        }
        System.out.println("okay!");


    }
}

class DependencyTree {
    private Map<Class<?>, ClassDependency> internalMap = new HashMap<>();
    private boolean resolved = false;

    public void resolve(Map<Class, Class[]> rawMap) {
        Map<Class<?>, ClassDependency> candidate = new HashMap<>();
        for (Class clazz : rawMap.keySet()) {
            candidate.put(clazz, recursiveResolver(clazz, rawMap));
        }
        internalMap = candidate;
        resolved = true;
    }


    public ClassDependency recursiveResolver(Class clazz, Map<Class, Class[]> rawMap) {
        Class[] dependenciesArray = rawMap.get(clazz);
        if (rawMap.get(clazz).length == 0) {
            return new ClassDependency(clazz, List.of());
        }
        List<ClassDependency> dependencies = new ArrayList<>();
        for (Class iteratingClass : dependenciesArray) {
            dependencies.add(recursiveResolver(iteratingClass, rawMap));
        }
        return new ClassDependency(clazz, dependencies);
    }

    public Map<Class<?>, ClassDependency> getInternalMap() {
        return this.internalMap;
    }


}

class Shell {
    // Spell book theme instead?
    Map<String, Object> instanceMap;

    public Shell(){
        this.instanceMap = new HashMap<>();
    }

    public void seal(String identifier, Object instance){
        instanceMap.put(identifier, instance);
    }

    public Object summon(String identifier){
        return instanceMap.get(identifier);
    }

    public boolean contains(String identifier){
        return instanceMap.get(identifier) != null;
    }
}

class ClassDependency<T> {
    final Class<T> clazz;
    final List<ClassDependency> dependencies;

    @Override
    public boolean equals(Object object) {
        if (object instanceof ClassDependency) {
            clazz.equals(((ClassDependency) object).getClazz());
        }
        return false;
    }

    /**
     * Returns an instance of the Class the ClassDependency represents.
     * @param shell
     * @return
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws InvocationTargetException
     */
    public T initialiseInstance(Shell shell) throws IllegalAccessException, NoSuchMethodException, InstantiationException, InvocationTargetException {
        //May need to break this up and rename
        if (dependencies.size() == 0) {
            if(shell.contains(clazz.toString())){
                return (T) shell.summon(clazz.toString());
            }
            T object = clazz.getConstructor().newInstance();
            shell.seal(clazz.toString(), object);
            return object;
        } else {
            Object[] initArgs = new Object[dependencies.size()];
            Class[] initArgsTypes = new Class[dependencies.size()];
            for (int i = 0; i < dependencies.size(); i++) {
                initArgs[i] = dependencies.get(i).initialiseInstance(shell);
                initArgsTypes[i] = dependencies.get(i).getClazz();
            }
            // Need initialisaztion saving here
            return clazz.getConstructor(initArgsTypes).newInstance(initArgs);
        }
    }

    public ClassDependency(Class<T> clazz, List<ClassDependency> dependencies) {
        this.clazz = clazz;
        this.dependencies = dependencies;
    }

    public Class<?> getClazz() {
        return this.clazz;
    }

    public List<ClassDependency> getDependencies() {
        return this.dependencies;
    }

    public boolean isLeaf() {
        return dependencies.size() == 0;
    }

    public boolean isBranch() {
        return dependencies.size() != 0;
    }

}

class ClassFinder {
    public List<String> classFinder(String root) throws IOException {
        List<String> classes = new ArrayList<>();
        Set<Path> paths = Files.list(new File(root).toPath()).collect(Collectors.toSet());
        for (Path p : paths) {
            if ((new File(p.toString())).isDirectory()) {
                classes.addAll(classFinder(p.toString()));
            } else {
                if (p.toString().endsWith(".java")) {
                    classes.add(getFullClassName(p.toString()));
                }
            }
            System.out.println(p.toString());
        }
        return classes;
    }

    private String getFullClassName(String javaFilePath) throws IOException {
        File file = new File(javaFilePath);

        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        st = br.readLine();
        while (!st.contains("package")) {
            st = br.readLine();
            if (st == null) {
                return null;
            }
        }
        String packageName = st.trim().substring(8, st.length() - 1);
        String className = file.getName().substring(0, file.getName().length() - 5);
        return (new StringBuilder()).append(packageName).append(".").append(className).toString();
    }
}

