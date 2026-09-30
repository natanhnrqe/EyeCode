package com.eyecode.language.navigation;

import java.nio.file.Path;
import java.util.List;

public interface NavigationService {

    List<NavigationTarget> definition(Path file, String source, long version, int line, int column);

    List<NavigationTarget> references(Path file, String source, long version, int line, int column,
                                      boolean includeDeclaration);
}
