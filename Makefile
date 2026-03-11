VERSION ?= nightly-SNAPSHOT
MODEL ?= .
SKIPTESTS ?= false
PACKAGE ?=
ARGS ?=
LOCALE ?=

MVN := ./mvnw
MVN_ARGS := -B -ntp -Drevision=$(VERSION) -f $(MODEL) $(ARGS)
DEPENDENCY_FILTER := $(if $(strip $(PACKAGE)),-Dincludes=$(PACKAGE),)
HELP_LOCALE := $(strip $(if $(LOCALE),$(LOCALE),$(LANG)))
HELP_TARGET := $(if $(filter zh%,$(HELP_LOCALE)),help-zh,help-en)

.PHONY: help help-en help-zh clean validate verify test package install flatten refresh dependencies show-version ci initial-git install-precommit doc format deploy

help:
	@$(MAKE) --no-print-directory $(HELP_TARGET)

help-en:
	@echo "IntentForge Make targets"
	@echo ""
	@echo "Common variables:"
	@echo "  VERSION=<revision>     Default: nightly-SNAPSHOT"
	@echo "  MODEL=<path>           Default: ., or a module directory / pom.xml"
	@echo "  SKIPTESTS=<bool>       Default: false"
	@echo "  PACKAGE=<gav-pattern>  Filter for dependency:tree"
	@echo "  ARGS='<extra args>'    Extra arguments passed to mvn/mvnw"
	@echo "  LOCALE=<en|zh>         Force help output language"
	@echo ""
	@echo "Available targets:"
	@echo "  make validate          Validate the reactor"
	@echo "  make test              Run tests"
	@echo "  make package           Package the current MODEL"
	@echo "  make install           Install artifacts to the local repository"
	@echo "  make flatten           Generate flattened POM files"
	@echo "  make refresh           Refresh dependencies with -U"
	@echo "  make dependencies      Show dependency tree, optional PACKAGE=groupId:artifactId"
	@echo "  make show-version      Print project.version"
	@echo "  make ci                Run local CI steps: validate + package(skip tests)"
	@echo "  make clean             Remove target directories and .flattened-pom.xml"
	@echo ""
	@echo "Examples:"
	@echo "  make help"
	@echo "  make help LOCALE=zh"
	@echo "  LANG=zh_CN.UTF-8 make help"
	@echo "  make package SKIPTESTS=true"
	@echo "  make install MODEL=intentforge-api"

help-zh:
	@echo "IntentForge Make 命令"
	@echo ""
	@echo "常用变量:"
	@echo "  VERSION=<revision>     默认: nightly-SNAPSHOT"
	@echo "  MODEL=<path>           默认: .，也可指定模块目录或 pom.xml"
	@echo "  SKIPTESTS=<bool>       默认: false"
	@echo "  PACKAGE=<gav-pattern>  用于 dependency:tree 过滤"
	@echo "  ARGS='<extra args>'    透传给 mvn/mvnw 的额外参数"
	@echo "  LOCALE=<en|zh>         强制指定 help 输出语言"
	@echo ""
	@echo "可用命令:"
	@echo "  make validate          校验 reactor"
	@echo "  make test              执行测试"
	@echo "  make package           打包当前 MODEL"
	@echo "  make install           安装到本地仓库"
	@echo "  make flatten           生成 flatten POM"
	@echo "  make refresh           使用 -U 刷新依赖"
	@echo "  make dependencies      查看依赖树，可配 PACKAGE=groupId:artifactId"
	@echo "  make show-version      输出 project.version"
	@echo "  make ci                本地执行 CI 步骤: validate + package(跳过测试)"
	@echo "  make clean             清理 target 和 .flattened-pom.xml"
	@echo ""
	@echo "示例:"
	@echo "  make help"
	@echo "  make help LOCALE=en"
	@echo "  LANG=zh_CN.UTF-8 make help"
	@echo "  make package SKIPTESTS=true"
	@echo "  make install MODEL=intentforge-api"

clean:
	$(MVN) $(MVN_ARGS) clean -Dmaven.test.skip=true

validate:
	$(MVN) $(MVN_ARGS) validate

verify:
	$(MVN) $(MVN_ARGS) clean verify -DskipTests=$(SKIPTESTS)

test:
	$(MVN) $(MVN_ARGS) test -DskipTests=false

package:
	$(MVN) $(MVN_ARGS) clean package -DskipTests=$(SKIPTESTS)

install:
	$(MVN) $(MVN_ARGS) clean install -DskipTests=$(SKIPTESTS)

flatten:
	$(MVN) $(MVN_ARGS) flatten:flatten -DskipTests=true

refresh:
	$(MVN) $(MVN_ARGS) dependency:resolve -U

dependencies:
	$(MVN) $(MVN_ARGS) dependency:tree $(DEPENDENCY_FILTER)

show-version:
	$(MVN) $(MVN_ARGS) help:evaluate -Dexpression=project.version -DforceStdout

ci:
	$(MVN) $(MVN_ARGS) validate
	$(MVN) $(MVN_ARGS) package -DskipTests=true

initial-git:
	@if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then \
		echo "Git repository already exists. Skipping initialization."; \
	else \
		echo "Initializing Git repository..."; \
		git init -b main; \
		git add .; \
		git commit -m "chore: initial commit"; \
	fi

install-precommit:
	@if [ -f .pre-commit-config.yaml ] || [ -f .pre-commit-config.yml ]; then \
		echo "[pre-commit] Installing hooks..."; \
		pre-commit install --install-hooks; \
		echo "[pre-commit] Updating hooks..."; \
		pre-commit autoupdate; \
	else \
		echo "No .pre-commit-config.yaml found. Skipping pre-commit installation."; \
	fi

doc:
	@echo "The project does not configure a documentation generation plugin yet. The doc target is unavailable."
	@exit 1

format:
	@echo "The project does not configure a formatting plugin yet. The format target is unavailable."
	@exit 1

deploy:
	@echo "The project does not configure repository publishing or a deploy pipeline yet. The deploy target is unavailable."
	@exit 1
