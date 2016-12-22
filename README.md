# Liferay Theme Live-deploy

This small maven plugin helps to develop Liferay themes faster. While a full compilation with Liferay's maven plugin and Liferay hot deploy takes at least one minute, this plugin allow you to deploy your theme in seconds. This will save a lot of your time !

This plugin was not-yet fully tested in a professional context but it is based on a well-proven personnal script I written based on [this article from the Liferay's blog](https://web.liferay.com/fr/web/marcus.hjortzen/blog/-/blogs/fast-development-using-compass-sass-and-liferay-portal) and I used for Liferay 6.2 development. So, afterward I written this maven plugin in order to make this more reliable. This is only intended to be used during development phase and with a Liferay bundled with Tomcat.

Please report any bugs.

## Quick start

Note that the artifact is not yet available in maven central repository. Meanwhile I suggest you to add the source to your maven build in order to make it available in your local repository.

To use it, simply add the plugin to your build:

```xml
<build>
	<plugins>
		<plugin>
			<groupId>fr.gugau.lfr.tools</groupId>
			<artifactId>lfr-theme-livedeploy</artifactId>
			<version>${livedeploy.version}</version>
		</plugin>
	</plugins>
</build>
```

And run the following command to deploy your theme :

```bash
mvn lfr-theme-livedeploy:live-deploy
```


## Usage

This plugin deploy a theme by copying files into the deployed theme directory, so you must configure your Liferay in **[developer mode](https://web.liferay.com/fr/community/wiki/-/wiki/Main/Liferay+Developer+Mode)** in order to disable caches and the theme must have been deployed normally before. If you respect theme development best pratices, you have nothing to configure, the plugin will looks for theme folders (templates, css, js and images) and copy them into the deployed theme by using the liferay.app.server.deploy.dir property (the one used by liferay-maven-plugin).

In other cases, here are basic options:

| Name | Default value | Description |
| ---- |:-------------:| ----------- |
| serverDeployDir | null | The webapp directory of your Liferay bundle (if you want to override liferay.app.server.deploy.dir property) |
| staticContentFiles | templates, js, images | The list of all resources that must be deployed (except css directory) |
| sassSourceFiles | customs.css | The list of SASS files that must be compiled (root files, don't add included files in here) |

There is two types of live-deployment: copy and compile. The first one is used by default and make a simple copy of theme files, the compilation to CSS is done by Liferay. The second one compile SASS files using the binary sass compiler that **must be installed** on your system, it allows you to deal with advanced options that are helpfull and compile files with line number comments automatically (sass compiler "--line-numbers" option). It may also be faster than delegating SASS compilation to Liferay. Here are advanced options:

| Name | Default value | Description |
| ---- |:-------------:| ----------- |
| method | copy | Live-deploy method for SASS files. Either "copy" (SASS files are compiled by Liferay) or "compile" (use sass executable compiler). |
| sassCompilerCommand | sass | Sass compiler executable, if it's in the PATH you can keep the default value |
| sassCompilerStyle | expanded | Passed as "--style" option to the compiler |
| sassCompilerSourceMap | none | Passed as "--sourcemap" option to the compiler |

## Limitations

You can't use this plugin to redeploy files in WEB-INF folder because Liferay will not look for changes in these files. It's goal is only to speed up CSS/HTML creation directly into Liferay.
