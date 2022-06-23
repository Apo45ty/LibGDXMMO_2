package games.apolion.controls;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.Vector3;

public class MyFirstPersonCameraController extends FirstPersonCameraController {
    private final PerspectiveCamera cam;

    public MyFirstPersonCameraController(PerspectiveCamera camera) {
        super(camera);
        this.cam = camera;
    }
    private int mouseX = 0;
    private int mouseY = 0;
    private float rotSpeed = 0.2f;

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        int magX = Math.abs(mouseX - screenX);
        int magY = Math.abs(mouseY - screenY);

        if (mouseX > screenX) {
            cam.rotate(Vector3.Y, 1 * magX * rotSpeed);
            cam.update();
        }

        if (mouseX < screenX) {
            cam.rotate(Vector3.Y, -1 * magX * rotSpeed);
            cam.update();
        }

        if (mouseY < screenY) {
            if (cam.direction.y > -0.965)
                cam.rotate(cam.direction.cpy().crs(Vector3.Y), -1 * magY * rotSpeed);
            cam.update();
        }

        if (mouseY > screenY) {

            if (cam.direction.y < 0.965)
                cam.rotate(cam.direction.cpy().crs(Vector3.Y), 1 * magY * rotSpeed);
            cam.update();
        }

        mouseX = screenX;
        mouseY = screenY;

        return false;
    }
}
