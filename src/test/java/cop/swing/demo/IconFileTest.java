package cop.swing.demo;

import cop.icoman.IconFile;
import cop.icoman.IconIO;
import cop.icoman.IconManager;
import cop.icoman.exceptions.IconManagerException;
import cop.icoman.ico.IcoFile;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Cherednik
 * @since 30.10.2016
 */
public class IconFileTest {
    @BeforeClass
    public static void init() {
        IconManager.getInstance();
    }

    @Test
    public void testIconFile() throws IOException, IconManagerException {
        IconFile iconFile = IconIO.read(ImageIO.createImageInputStream(IconFileTest.class.getResourceAsStream("/test.ico")));
        assertThat(iconFile).isNotNull();
        assertThat(iconFile).isInstanceOf(IcoFile.class);
        assertThat(iconFile.getTotalImages()).isEqualTo(16);

        Set<String> ids = iconFile.getIds();
        assertThat(ids).hasSize(16);

        String id = Utils.getAt(ids.iterator(), 3);
        assertThat(id).isEqualTo("16x16_256");

        Image expectedImage = iconFile.getImage(id);
        assertThat(expectedImage).isInstanceOf(BufferedImage.class);
        assertThat(((RenderedImage)expectedImage).getWidth()).isEqualTo(16);
        assertThat(((RenderedImage)expectedImage).getHeight()).isEqualTo(16);
        assertThat(Utils.getAt(iconFile.iterator(), 3)).isSameAs(expectedImage);
        assertThat(iconFile.getImage("16x16_256")).isSameAs(expectedImage);
    }
}
