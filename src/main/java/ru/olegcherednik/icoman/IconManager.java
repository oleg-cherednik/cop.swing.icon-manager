package ru.olegcherednik.icoman;

import ru.olegcherednik.icoman.exceptions.IconDuplicationException;
import ru.olegcherednik.icoman.exceptions.IconManagerException;
import ru.olegcherednik.icoman.exceptions.IconNotFoundException;
import ru.olegcherednik.icoman.icl.imageio.IclReaderSpi;
import ru.olegcherednik.icoman.icns.imageio.IcnsReaderSpi;
import ru.olegcherednik.icoman.ico.imageio.IcoReaderSpi;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.stream.ImageInputStream;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Oleg Cherednik
 * @since 01.09.2013
 */
public final class IconManager {
    private static final IconManager INSTANCE = new IconManager();

    private final Map<String, IconFile> icons = new LinkedHashMap<>();

    public static IconManager getInstance() {
        return INSTANCE;
    }

    static {
        IcoReaderSpi.register();
        IclReaderSpi.register();
        IcnsReaderSpi.register();
    }

    private IconManager() {
    }

    public Set<String> getIds() {
        return icons.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(icons.keySet());
    }

    @NotNull
    public IconFile addIcon(String id, ImageInputStream in) throws IconManagerException, IOException {
        if (in == null)
            throw new IOException(String.format("Resource '%s' doesn't exists", id));
        return addIcon(id, IconIO.read(in));
    }

    public IconFile addIcon(String id, IconFile icon) throws IconManagerException {
        if (StringUtils.isBlank(id) || icon == null)
            throw new IconManagerException("id/icon is not set");
        if (icons.containsKey(id))
            throw new IconDuplicationException(id);

        icons.put(id, icon);

        return icon;
    }

    public void removeIcon(String id) {
        icons.remove(id);
    }

    @NotNull
    public <T extends IconFile> T getIconFile(String id) throws IconNotFoundException {
        IconFile icon = icons.get(id);

        if (icon == null)
            throw new IconNotFoundException(id);

        return (T)icon;
    }
}
