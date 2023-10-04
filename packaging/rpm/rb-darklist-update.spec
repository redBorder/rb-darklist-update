Name:     darklist-updated
Version:  %{__version}
Release:  %{__release}%{?dist}

License:  GNU AGPLv3
URL:  https://github.com/redBorder/rb-darklist-update.git
Source0: %{name}-%{version}.tar.gz

BuildRequires: maven java-devel

%global debug_package %{nil}

Summary: rb-darklist-update package
Requires: java

%description
%{summary}

%prep
%setup -qn %{name}-%{version}

%build
export MAVEN_OPTS="-Xmx512m -Xms256m -Xss10m" && mvn clean package

%install
mkdir -p %{buildroot}/usr/lib/%{name}
install -D -m 644 target/*.jar %{buildroot}/usr/lib/%{name}/%{name}.jar

%clean
rm -rf %{buildroot}

%pre

%post -p /sbin/ldconfig
%postun -p /sbin/ldconfig

%files
%defattr(644,root,root)
/usr/lib/%{name}

%changelog
* Wed Oct 4 2023 David Vanhoucke <dvanhoucke@redborder.com> - 1.0.0-1
- update spec
* Tue Mar 29 2022 Eduardo Reyes <eareyes@redborder.com> - 0.0.1-1
- first spec version
