{ nixpkgs ? /etc/nixos/nixpkgs
, mobl ? { outPath = ./.; rev = 1234; }
, moblPlugin ? { outPath = ../mobl-plugin ; rev = 1234; }
, hydraConfig ? { outPath = ../hydra-config ; rev = 1234; }
}: 
let
  pkgs = import nixpkgs { system = "x86_64-linux" ; };

  jars = {
    aster = 
      pkgs.fetchurl {
        url = http://zef.me/mobl/aster.jar;
        sha256 = "1jsk6vidmvlnw2hjqs6syvv8fniapn6qxlx2va8hwpf3px49i103";
      } ;
    make_permissive = 
      pkgs.fetchurl {
        url = http://zef.me/mobl/make_permissive.jar;
        sha256 = "1m7sny5dxg2ga0ac4irqmbw6jqqg9bxf8qbrj3a317dinllfsvhg";
      } ;
    sdf2imp = 
      pkgs.fetchurl {
        url = http://zef.me/mobl/sdf2imp.jar;
        sha256 = "128j8waw3mp6iwjd7akd1fy8i7hd4bppvjlnvwpmf6x0mhc11v3a";
      } ;
    strategoxt = 
      pkgs.fetchurl {
        url = http://zef.me/mobl/strategoxt.jar;
        sha256 = "1xh2k0cds8m1hns90l99rxhb2imgc16mfdlkhlfsan0ygcidjgdf";
      } ;
    strategomix = 
      pkgs.fetchurl {
        url = http://zef.me/mobl/StrategoMix.def;
        sha256 = "1dcki0sip6a9ng220px1n4dwfx6b7kdslkix1ld1r2apjnx6xz4n";
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

      buildInputs = [pkgs.strategoPackages.sdf];

      jarWrappers = [ { name = "moblc"; jar = "moblc.jar"; mainClass = "trans.Main"; } ];
      jars = ["./moblc.jar"];

      # add ecj to classpath of ant
      ANT_ARGS="-lib ${pkgs.ecj}/lib/java";

      LOCALCLASSPATH = "utils/aster.jar:utils/make_permissive.jar:utils/sdf2imp.jar:utils/strategoxt.jar";

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
        cp -vf `find $eclipse -name StrategoMix.def` mobl/utils/
        export LOCALCLASSPATH="utils/js.jar"
      '';
    };

    tests = {
      install = eclipseFun {
        name = "eclipse-mobl-install-test";
        src =  pkgs.fetchurl {
          url = http://download.springsource.com/release/ECLIPSE/helios/SR1/eclipse-SDK-3.6.1-linux-gtk.tar.gz ;
          sha256 = "0s48rjaswi8m5gan1zlqvfwb4l06x5nslkq41wpkrbyj9ka8gh4x";
        };
        updatesites = [ "file://${jobs.updatesite}/site" ];
        installIUs = [ "org.mobl_lang.feature.feature.group" ];
        dontInstall = true;
      };
    };

  };

in jobs

