package org.vaadin.cored;

//public class ProjectSkeletonUtil {
//	public enum SkeletonType {
//		NONE, VAADIN_APP
//	};
//
//	public static void applyProjectSkeletonTo(Project p, SkeletonType type) {
//		if (type == SkeletonType.VAADIN_APP) {
//			applyVaadinApp(p);
//		}
//	}
//
//	private static void applyVaadinApp(Project p) {
//		String cls = appClassNameFromProjectName(p.getName());
//		String ske = createVaadinSkeletonCode(p.getPackageName(), cls);
//		ProjectFile f = new ProjectFile(ProjectFile.dirFromPackage(p.getPackageName()), cls+".java");
//		p.createDoc(f, ske);
//	}
//
//	private static String appClassNameFromProjectName(String name) {
//		return name.substring(0, 1).toUpperCase()
//				+ name.substring(1).toLowerCase() + "Application";
//	}
//
//	private static String createVaadinSkeletonCode(String pakkage, String cls) {
//		return "package "+pakkage+"\n\nimport com.vaadin.ui.Window;\n"
//				+ "import com.vaadin.ui.Label;\n\n" + "public class " + cls
//				+ " extends com.vaadin.Application {\n\n"
//				+ "    public void init() {\n"
//				+ "        Window main = new Window(\"" + cls + "\");\n"
//				+ "        setMainWindow(main);\n"
//				+ "        main.addComponent(new Label(\"This is " + cls
//				+ "\"));\n" + "    }\n\n" + "}\n";
//
//	}
//}
