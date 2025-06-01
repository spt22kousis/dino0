public class LevelObstacleData {
    double spawnTriggerX;
    double width;
    double height;
    double yPosition;
    String type;

    public LevelObstacleData(double spawnTriggerX, double width, double height, double yPosition, String type) {
        this.spawnTriggerX = spawnTriggerX;
        this.width = width;
        this.height = height;
        this.yPosition = yPosition;
        this.type = type;
    }
}