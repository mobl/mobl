{ nixpkgs ? /etc/nixos/nixpkgs
, mobl ? { outPath = ./.; rev = 1234; }
, moblPlugin ? { outPath = ../mobl-plugin ; rev = 1234; }
, hydraConfig ? { outPath = ../hydra-config ; rev = 1234; }
, webdslzips ? { outPath = ../webdsl-zips ; rev = 1234;}
}: 
let
  pkgs = import nixpkgs { system = "x86_64-linux" ; };
  maindevelopers = ["chrismelman@hotmail.com"];
  spoofaxgenerator = pkgs.fetchsvn{
			url = https://svn.strategoxt.org/repos/StrategoXT/spoofax-imp/trunk/org.strategoxt.imp.spoofax.generator;
			sha256 = "14y9akqar3bp692ckzmx7llc4axz4v77kcnqh4kwx74ngmkac3jd";
	  };
  jars = {
    aster = 
      pkgs.fetchurl {
        url = http://webdsl.org/mobldeps/aster.jar;
       sha256 = "08487bb0b82b2401661c2b53031cae6f20feaa8b0ceacf7735293de75444917f";
      } ;
    make_permissive = 
	pkgs.fetchurl {
      url = http://webdsl.org/mobldeps/make_permissive.jar;
       sha256 = "0541482cf686a3ed17efdf5b9d6edfa99447b0dee53c25df9fd6f408e5e4d175";
      } ;
    sdf2imp = 
      pkgs.fetchurl {
        url = http://webdsl.org/mobldeps/sdf2imp.jar;
       sha256 = "91646adad47ca66db0bbdd9a472ec659f8d4411d841c36b3f701a9e86a907f7f";
      } ;
    strategoxt = 
      pkgs.fetchurl {
        url = http://webdsl.org/mobldeps/strategoxt.jar;
        sha256 = "1974a1fd40518f19251ac646f9b06690e79e96fdb94a75ec5e015baa7425bbad";
      } ;
    strategomix = 
      pkgs.fetchurl {
        url = http://webdsl.org/mobldeps/StrategoMix.def;
        sha256 = "96fc6eba9557891c1a0d3d4eaadb3ccb74c71bb1a15f20c4b349991b358893b5";
      } ;
  }; 
  

  eclipseFun = import "${hydraConfig}/eclipse.nix" pkgs ;

  moblc = app :
    pkgs.stdenv.mkDerivation {
      name = "mobl-${app.name}-${mobl.rev}";
      src = mobl;
      buildInputs = [jobs.moblc];
      buildCommand = ''
        ensureDir $out/www
        ensureDir $out/nix-support
        ulimit -s unlimited
        cd $out/
        cp -Rv ${mobl}/samples/${app.name}/* .
        echo moblc -i ${app.app} -d www -O -I ${mobl}/stdlib ${if app ? stdlib then "-I ${app.stdlib}" else ""}
        moblc -i ${app.app} -d www -O -I ${mobl}/stdlib ${if app ? stdlib then "-I ${app.stdlib}" else ""}
        ln -s $out/www/`basename ${app.app} .mobl`.html $out/www/index.html
        echo "doc www $out/www" >> $out/nix-support/hydra-build-products
      '';
    };

  jobs = {
    manual = pkgs.stdenv.mkDerivation {
      name = "mobl-manual-${mobl.rev}";
      src = mobl;
      buildInputs = with pkgs; [perl htmldoc rubygems calibre];
      buildCommand = ''
        unpackPhase

        export HOME=`pwd`
        export GEM_HOME=`pwd`/gem
        export PATH=$PATH:$GEM_HOME/bin
        export RUBYLIB=${pkgs.rubygems}/lib

        gem install ronn

        cd $sourceRoot/manual
        make 

        ensureDir $out/nix-support
        cp -vR dist $out/
        ln -sv $out/dist/moblguide.html $out/dist/index.html
        echo "doc manual $out/dist" >> $out/nix-support/hydra-build-products
        echo "doc-pdf manual $out/dist/moblguide.pdf" >> $out/nix-support/hydra-build-products
        echo "doc mobi $out/dist/moblguide.mobi" >> $out/nix-support/hydra-build-products
        echo "doc epub $out/dist/moblguide.epub" >> $out/nix-support/hydra-build-products
      '';
      __noChroot = true;
    };

    moblc = with jars; pkgs.releaseTools.antBuild {
      name = "moblc-r${toString mobl.rev}";
      src = mobl;
      buildfile = "build.main.xml";      
      antTargets = ["moblc-release"];
      antProperties = [ 
        { name = "eclipse.spoofaximp.jars"; value = "utils/"; }
        { name = "build.compiler"; value = "org.eclipse.jdt.core.JDTCompilerAdapter"; }
        { name = "java.jar.enabled"; value = "true"; }
      ];
      
      spoofaxgenerator = pkgs.fetchsvn{
			url = https://svn.strategoxt.org/repos/StrategoXT/spoofax-imp/trunk/org.strategoxt.imp.spoofax.generator;
			sha256 = "14y9akqar3bp692ckzmx7llc4axz4v77kcnqh4kwx74ngmkac3jd";
	  };

      buildInputs = [pkgs.strategoPackages.sdf];

      jarWrappers = [ { name = "moblc"; jar = "moblc.jar"; mainClass = "trans.Main"; } ];
      jars = ["./moblc.jar"];

      # add ecj to classpath of ant
      ANT_ARGS="-lib ${pkgs.ecj}/lib/java";
      
      ANT_OPTS="-Declipse.spoofaximp.jars=utils/ -Xss8m -ss16m -Xmx1024m" ;

      LOCALCLASSPATH = "utils/aster.jar:utils/make_permissive.jar:utils/sdf2imp.jar:utils/strategoxt.jar:utils/nativebuildPhonegap.jar";

      preConfigure = ''
        ulimit -s unlimited
        mkdir -p utils
        cp -v ${aster} utils/aster.jar
        cp -v ${make_permissive} utils/make_permissive.jar
        cp -v ${strategoxt} utils/strategoxt.jar
        cp -v ${sdf2imp} utils/sdf2imp.jar
        cp -v ${strategomix} utils/StrategoMix.def
        ensureDir $out/bin
      '';
       meta.maintainers = maindevelopers;
    };

    samples = {
      controldemo        = moblc { name = "control-demo"; app = "demo.mobl"; } ;
      draw               = moblc { name = "draw"; app = "draw.mobl"; } ;
      geo                = moblc { name = "geo"; app = "maptest.mobl"; } ;
      helloserver_client = moblc { name = "helloserver"; app = "client.mobl"; } ;
      #helloserver_server = moblc { name = "helloserver"; app = "server.mobl"; stdlib = "${mobl}/stdlib-server-override"; } ;
      #irc_client         = moblc { name = "irc"; app = "irc.mobl"; } ;
      #irc_server         = moblc { name = "irc"; app = "server.mobl"; stdlib = "${mobl}/stdlib-server-override"; } ;
      shopping           = moblc { name = "shopping"; app = "shopping.mobl"; } ;
      tipcalculator      = moblc { name = "tipcalculator"; app = "tipcalculator.mobl"; } ;
      twittertrends      = moblc { name = "twittertrends"; app = "twittertrends.mobl"; } ;
      yql                = moblc { name = "yql"; app = "demo.mobl"; } ;
      todo               = moblc { name = "todo"; app = "todo.mobl"; } ;
      i18n                = moblc { name = "i18n"; app = "demo.mobl"; } ;
      #znake_client       = moblc { name = "znake"; app = "znake.mobl"; } ;
      #znake_server       = moblc { name = "znake"; app = "server.mobl"; } ;
    };      

    updatesite = import "${hydraConfig}/spoofax-fun.nix" {
      inherit pkgs;
      name = "mobl";
      version = "0.3.999";
      src = moblPlugin;
      buildInputs = [pkgs.strategoPackages.sdf];
      preConfigure = ''
        cp -Rv ${mobl} mobl
        chmod -R a+w mobl
        mkdir -p mobl/utils
        export LOCALCLASSPATH="utils/js.jar"
      '';
      meta.maintainers = maindevelopers;
    };

    tests = {
      install = eclipseFun {
        name = "eclipse-mobl-install-test";
        src =  pkgs.fetchurl {
          	url = http://download.springsource.com/release/ECLIPSE/indigo/SR2/eclipse-SDK-3.7.2-linux-gtk.tar.gz ;
     		sha256 = "f2cce7db448fa1209452605a653d82b7db17a844a86ed3bdb07e265a483c56c7";
        };
        updatesites = [ "file://${jobs.updatesite}/site" ];
        installIUs = [ "org.mobl_lang.feature.feature.group" ];
        dontInstall = true;
       
      };
      
    };
 zips = (import "${webdslzips}/eclipse.nix" {
 
  basename = "mobl-r${toString mobl.rev}";
  updatesites = [
    "http://hydra.nixos.org/job/mobl/master/updatesite/latest/download/2/site"
    "http://www.lclnet.nl/update"
    "http://download.eclipse.org/releases/indigo"
  ];
  installIUs = [
    "org.mobl_lang.feature.feature.group"
  ];  
   extraBuildInputs = [jobs.updatesite];
}).zips;
  
  };

in jobs

