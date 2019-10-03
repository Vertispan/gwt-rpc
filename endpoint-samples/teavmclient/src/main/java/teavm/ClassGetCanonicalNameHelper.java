package teavm;

public class ClassGetCanonicalNameHelper {
    public String getCName(Class<?> clazz) {
        if (clazz.isArray()) {
            return getCName(clazz.getComponentType()) + "[]";
        }
        if (clazz.getEnclosingClass() != null) {
            return getCName(clazz.getEnclosingClass()) + "." + clazz.getSimpleName();
        }
        return clazz.getName();
    }
}
