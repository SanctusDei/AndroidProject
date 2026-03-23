package Model;

public class MenuItem {
    private String title;
    private Class<?> targetActivity;

    public  MenuItem(String title, Class<?> targetActivity) {
        this.title = title;
        this.targetActivity = targetActivity;


    }

    // Getter 方法 (RecyclerView 适配器用到)
    public  String getTitle() {return title;}
    public  Class<?> getTargetActivity() {return  targetActivity;}

}
